package com.nan.stash.ui.util

import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.CategorySummary
import com.nan.stash.data.model.DayTrend
import com.nan.stash.data.model.MonthComparison
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WalletBalances(
    val onlineBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
) {
    val netBalance: Double get() = onlineBalance + cashBalance
}

object AnalyticsCalculator {

    fun calculateWalletBalances(transactions: List<TransactionEntity>): WalletBalances {
        var online = 0.0
        var cash = 0.0
        var income = 0.0
        var expense = 0.0
        for (t in transactions) {
            val isOnline = t.paymentMode == PaymentMode.ONLINE
            when (t.type) {
                TransactionType.INCOME -> {
                    income += t.amount
                    if (isOnline) online += t.amount else cash += t.amount
                }
                TransactionType.EXPENSE -> {
                    expense += t.amount
                    if (isOnline) online -= t.amount else cash -= t.amount
                }
                TransactionType.TRANSFER -> {
                    if (isOnline) {
                        online -= t.amount
                        cash += t.amount
                    } else {
                        cash -= t.amount
                        online += t.amount
                    }
                }
                TransactionType.LEND -> {
                    if (isOnline) online -= t.amount else cash -= t.amount
                }
                TransactionType.REPAYMENT -> {
                    if (isOnline) online += t.amount else cash += t.amount
                }
            }
        }
        return WalletBalances(
            onlineBalance = online,
            cashBalance = cash,
            totalIncome = income,
            totalExpense = expense
        )
    }

    fun isLowBalance(balance: Double, threshold: Double, enabled: Boolean): Boolean {
        return enabled && threshold > 0.0 && balance <= threshold
    }

    fun calculateCategorySummaries(transactions: List<TransactionEntity>): List<CategorySummary> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenses.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        return expenses.groupBy { it.tag }
            .map { (tag, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummary(
                    tag = tag,
                    amount = sum,
                    percentage = (sum / totalExpense * 100).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    fun calculateWeeklyTrends(transactions: List<TransactionEntity>): List<DayTrend> {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val fullDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val nowCal = Calendar.getInstance()
        val currentDayIndex = (nowCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 .. Sunday = 6

        // Compute start of Monday of the current week
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -currentDayIndex)
        }

        return (0..6).map { i ->
            val dayCal = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = (dayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            val dayTransactions = transactions.filter { it.timestamp in startOfDay until endOfDay }
            val income = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = income - expense
            val hasActivity = dayTransactions.isNotEmpty()

            DayTrend(
                dayLabel = days[i],
                fullDayName = fullDays[i],
                isToday = (i == currentDayIndex),
                income = income,
                expense = expense,
                net = net,
                amount = Math.abs(net),
                isPositive = net >= 0,
                hasActivity = hasActivity
            )
        }
    }

    fun calculateMonthComparison(transactions: List<TransactionEntity>): MonthComparison {
        val now = Calendar.getInstance()
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) // 0-based

        val curMonthStart = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nextMonthStart = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val prevMonthCal = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }
        val prevMonthStart = prevMonthCal.timeInMillis

        val curMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(curMonthStart))
        val prevMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(prevMonthStart))

        val curTx = transactions.filter { it.timestamp in curMonthStart until nextMonthStart }
        val prevTx = transactions.filter { it.timestamp in prevMonthStart until curMonthStart }

        val curIncome = curTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val curExpense = curTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val curNet = curIncome - curExpense

        val prevIncome = prevTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExpense = prevTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevNet = prevIncome - prevExpense

        val expenseDiffPct = if (prevExpense > 0.0) {
            (((curExpense - prevExpense) / prevExpense) * 100).toFloat()
        } else if (curExpense > 0.0) {
            100f
        } else 0f

        val incomeDiffPct = if (prevIncome > 0.0) {
            (((curIncome - prevIncome) / prevIncome) * 100).toFloat()
        } else if (curIncome > 0.0) {
            100f
        } else 0f

        return MonthComparison(
            currentMonthLabel = curMonthName,
            previousMonthLabel = prevMonthName,
            currentIncome = curIncome,
            previousIncome = prevIncome,
            currentExpense = curExpense,
            previousExpense = prevExpense,
            currentNet = curNet,
            previousNet = prevNet,
            expenseDiffPercentage = expenseDiffPct,
            incomeDiffPercentage = incomeDiffPct,
            hasComparisonData = curTx.isNotEmpty() || prevTx.isNotEmpty()
        )
    }
}
