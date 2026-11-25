package com.nan.stash.data.model

data class CategorySummary(
    val tag: String,
    val amount: Double,
    val percentage: Float
)

data class DayTrend(
    val dayLabel: String,
    val fullDayName: String = "",
    val isToday: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
    val amount: Double = 0.0,
    val isPositive: Boolean = true,
    val hasActivity: Boolean = false
)

data class MonthComparison(
    val currentMonthLabel: String = "",
    val previousMonthLabel: String = "",
    val currentIncome: Double = 0.0,
    val previousIncome: Double = 0.0,
    val currentExpense: Double = 0.0,
    val previousExpense: Double = 0.0,
    val currentNet: Double = 0.0,
    val previousNet: Double = 0.0,
    val expenseDiffPercentage: Float = 0f,
    val incomeDiffPercentage: Float = 0f
)
