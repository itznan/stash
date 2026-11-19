package com.nan.stash.ui.util

import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnalyticsCalculatorTest {

    @Test
    fun categorySummaries_emptyList_returnsEmpty() {
        val summaries = AnalyticsCalculator.calculateCategorySummaries(emptyList())
        assertTrue(summaries.isEmpty())
    }

    @Test
    fun categorySummaries_calculatesCorrectPercentages() {
        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 60.0, tag = "Food"),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 40.0, tag = "Transport"),
            TransactionEntity(id = 3, type = TransactionType.INCOME, amount = 200.0, tag = "Salary")
        )

        val summaries = AnalyticsCalculator.calculateCategorySummaries(txs)
        assertEquals(2, summaries.size)

        val food = summaries.first { it.tag == "Food" }
        assertEquals(60.0, food.amount, 0.001)
        assertEquals(60.0f, food.percentage, 0.001f)

        val transport = summaries.first { it.tag == "Transport" }
        assertEquals(40.0, transport.amount, 0.001)
        assertEquals(40.0f, transport.percentage, 0.001f)
    }

    @Test
    fun weeklyTrends_producesSevenDaysWithCorrectIndex() {
        val trends = AnalyticsCalculator.calculateWeeklyTrends(emptyList())
        assertEquals(7, trends.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), trends.map { it.dayLabel })
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), trends.map { it.fullDayName })
        // Exactly one day should be marked isToday
        assertEquals(1, trends.count { it.isToday })
    }

    @Test
    fun weeklyTrends_aggregatesIncomeAndExpenseProperly() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 100.0, tag = "Salary", timestamp = nowCal.timeInMillis),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 30.0, tag = "Food", timestamp = nowCal.timeInMillis)
        )

        val trends = AnalyticsCalculator.calculateWeeklyTrends(txs)
        val todayTrend = trends.first { it.isToday }

        assertEquals(100.0, todayTrend.income, 0.001)
        assertEquals(30.0, todayTrend.expense, 0.001)
        assertEquals(70.0, todayTrend.net, 0.001)
        assertTrue(todayTrend.isPositive)
        assertTrue(todayTrend.hasActivity)
    }

    @Test
    fun monthComparison_calculatesDifferencesProperly() {
        val curCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val prevCal = (curCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }

        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 150.0, tag = "Bills", timestamp = curCal.timeInMillis),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 100.0, tag = "Bills", timestamp = prevCal.timeInMillis)
        )

        val comparison = AnalyticsCalculator.calculateMonthComparison(txs)
        assertEquals(150.0, comparison.currentExpense, 0.001)
        assertEquals(100.0, comparison.previousExpense, 0.001)
        // (150 - 100) / 100 * 100 = +50%
        assertEquals(50.0f, comparison.expenseDiffPercentage, 0.01f)
    }
}
