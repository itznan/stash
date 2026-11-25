package com.nan.stash.ui.util

import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.CategorySummary
import com.nan.stash.data.model.DayTrend
import com.nan.stash.data.model.MonthComparison
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class WalletBalances(
    val onlineBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
) {
    val netBalance: Double
        get() = AnalyticsCalculator.fromCents(
            AnalyticsCalculator.toCents(onlineBalance) + AnalyticsCalculator.toCents(cashBalance)
        )
}

object AnalyticsCalculator {

    fun toCents(amount: Double): Long = (amount * 100.0).roundToLong()

    fun fromCents(cents: Long): Double = cents / 100.0

    fun formatSignedCurrency(
        amount: Double,
        currencySymbol: String,
        formatter: NumberFormat? = null,
        showPlus: Boolean = false
    ): String {
        val cents = toCents(amount)
        val sign = when {
            cents < 0L -> "-"
            showPlus -> "+"
            else -> ""
        }
        val absVal = fromCents(abs(cents))
        val formatted = formatter?.format(absVal) ?: String.format(Locale.US, "%.2f", absVal)
        return "$sign$currencySymbol$formatted"
    }

    fun applyRepayment(loan: LoanEntity, amountRepaid: Double): LoanEntity {
        val newRemainingCents = (toCents(loan.remainingAmount) - toCents(amountRepaid)).coerceAtLeast(0L)
        return loan.copy(
            remainingAmount = fromCents(newRemainingCents),
            isSettled = newRemainingCents == 0L
        )
    }

    fun applyAdditionalLoan(loan: LoanEntity, additionalAmount: Double): LoanEntity {
        val addCents = toCents(additionalAmount)
        return loan.copy(
            totalAmount = fromCents(toCents(loan.totalAmount) + addCents),
            remainingAmount = fromCents(toCents(loan.remainingAmount) + addCents),
            isSettled = false
        )
    }

    fun calculateWalletBalances(transactions: List<TransactionEntity>): WalletBalances {
        var onlineCents = 0L
        var cashCents = 0L
        var incomeCents = 0L
        var expenseCents = 0L
        for (t in transactions) {
            val cents = toCents(t.amount)
            val isOnline = t.paymentMode == PaymentMode.ONLINE
            when (t.type) {
                TransactionType.INCOME -> {
                    incomeCents += cents
                    if (isOnline) onlineCents += cents else cashCents += cents
                }
                TransactionType.EXPENSE -> {
                    expenseCents += cents
                    if (isOnline) onlineCents -= cents else cashCents -= cents
                }
                TransactionType.TRANSFER -> {
                    if (isOnline) {
                        onlineCents -= cents
                        cashCents += cents
                    } else {
                        cashCents -= cents
                        onlineCents += cents
                    }
                }
                TransactionType.LEND -> {
                    if (isOnline) onlineCents -= cents else cashCents -= cents
                }
                TransactionType.REPAYMENT -> {
                    if (isOnline) onlineCents += cents else cashCents += cents
                }
            }
        }
        return WalletBalances(
            onlineBalance = fromCents(onlineCents),
            cashBalance = fromCents(cashCents),
            totalIncome = fromCents(incomeCents),
            totalExpense = fromCents(expenseCents)
        )
    }

    fun isLowBalance(balance: Double, threshold: Double, enabled: Boolean): Boolean {
        return enabled && toCents(threshold) > 0L && toCents(balance) <= toCents(threshold)
    }

    private fun monthBounds(): Triple<Long, Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val curMonthStart = cal.timeInMillis
        val nextMonthStart = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
        val prevMonthStart = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }.timeInMillis
        return Triple(prevMonthStart, curMonthStart, nextMonthStart)
    }

    fun calculateCategorySummaries(transactions: List<TransactionEntity>): List<CategorySummary> {
        val (_, curMonthStart, nextMonthStart) = monthBounds()
        val totalsByTag = LinkedHashMap<String, Long>()
        var totalExpenseCents = 0L
        for (t in transactions) {
            if (t.type == TransactionType.EXPENSE && t.timestamp in curMonthStart until nextMonthStart) {
                val cents = toCents(t.amount)
                if (cents > 0L) {
                    totalExpenseCents += cents
                    totalsByTag[t.tag] = (totalsByTag[t.tag] ?: 0L) + cents
                }
            }
        }
        if (totalExpenseCents <= 0L) return emptyList()

        return totalsByTag.entries
            .map { (tag, sumCents) ->
                CategorySummary(
                    tag = tag,
                    amount = fromCents(sumCents),
                    percentage = (sumCents.toDouble() / totalExpenseCents.toDouble() * 100.0).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    fun calculateWeeklyTrends(transactions: List<TransactionEntity>): List<DayTrend> {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val fullDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val nowCal = Calendar.getInstance()
        val currentDayIndex = (nowCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 .. Sunday = 6

        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -currentDayIndex)
        }

        val dayStarts = LongArray(8)
        for (i in 0..7) {
            dayStarts[i] = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }.timeInMillis
        }

        val incomeCents = LongArray(7)
        val expenseCents = LongArray(7)
        val weekStart = dayStarts[0]
        val weekEnd = dayStarts[7]
        for (t in transactions) {
            if (t.timestamp !in weekStart until weekEnd) continue
            if (t.type != TransactionType.INCOME && t.type != TransactionType.EXPENSE) continue
            for (d in 0..6) {
                if (t.timestamp in dayStarts[d] until dayStarts[d + 1]) {
                    val cents = toCents(t.amount)
                    if (t.type == TransactionType.INCOME) incomeCents[d] += cents else expenseCents[d] += cents
                    break
                }
            }
        }

        return (0..6).map { i ->
            val inc = fromCents(incomeCents[i])
            val exp = fromCents(expenseCents[i])
            val netCents = incomeCents[i] - expenseCents[i]
            val net = fromCents(netCents)
            DayTrend(
                dayLabel = days[i],
                fullDayName = fullDays[i],
                isToday = (i == currentDayIndex),
                income = inc,
                expense = exp,
                net = net,
                amount = fromCents(abs(netCents)),
                isPositive = netCents >= 0L,
                hasActivity = incomeCents[i] > 0L || expenseCents[i] > 0L
            )
        }
    }

    fun calculateMonthComparison(transactions: List<TransactionEntity>): MonthComparison {
        val (prevMonthStart, curMonthStart, nextMonthStart) = monthBounds()

        var curIncomeCents = 0L
        var curExpenseCents = 0L
        var prevIncomeCents = 0L
        var prevExpenseCents = 0L
        for (t in transactions) {
            val cents = toCents(t.amount)
            when {
                t.timestamp in curMonthStart until nextMonthStart -> {
                    if (t.type == TransactionType.INCOME) curIncomeCents += cents
                    else if (t.type == TransactionType.EXPENSE) curExpenseCents += cents
                }
                t.timestamp in prevMonthStart until curMonthStart -> {
                    if (t.type == TransactionType.INCOME) prevIncomeCents += cents
                    else if (t.type == TransactionType.EXPENSE) prevExpenseCents += cents
                }
            }
        }

        val curIncome = fromCents(curIncomeCents)
        val curExpense = fromCents(curExpenseCents)
        val curNet = fromCents(curIncomeCents - curExpenseCents)

        val prevIncome = fromCents(prevIncomeCents)
        val prevExpense = fromCents(prevExpenseCents)
        val prevNet = fromCents(prevIncomeCents - prevExpenseCents)

        val expenseDiffPct = if (prevExpenseCents > 0L) {
            (((curExpenseCents - prevExpenseCents).toDouble() / prevExpenseCents) * 100.0).toFloat()
        } else if (curExpenseCents > 0L) {
            100f
        } else 0f

        val incomeDiffPct = if (prevIncomeCents > 0L) {
            (((curIncomeCents - prevIncomeCents).toDouble() / prevIncomeCents) * 100.0).toFloat()
        } else if (curIncomeCents > 0L) {
            100f
        } else 0f

        return MonthComparison(
            currentMonthLabel = DateUtils.formatMonthYear(curMonthStart),
            previousMonthLabel = DateUtils.formatMonthYear(prevMonthStart),
            currentIncome = curIncome,
            previousIncome = prevIncome,
            currentExpense = curExpense,
            previousExpense = prevExpense,
            currentNet = curNet,
            previousNet = prevNet,
            expenseDiffPercentage = expenseDiffPct,
            incomeDiffPercentage = incomeDiffPct
        )
    }
}
