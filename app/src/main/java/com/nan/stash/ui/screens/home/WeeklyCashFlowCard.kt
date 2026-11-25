package com.nan.stash.ui.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.model.DayTrend
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor
import com.nan.stash.ui.util.AnalyticsCalculator
import java.text.NumberFormat

@Composable
fun WeeklyCashFlowCard(
    weeklyTrends: List<DayTrend>,
    currencySymbol: String,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val initialIndex = remember(weeklyTrends) {
        val todayIdx = weeklyTrends.indexOfFirst { it.isToday }
        if (todayIdx >= 0) todayIdx else 0
    }
    var selectedDayIndex by remember { mutableIntStateOf(initialIndex) }
    val selectedDay = weeklyTrends.getOrNull(selectedDayIndex) ?: weeklyTrends.firstOrNull()

    val totalWeekNet = remember(weeklyTrends) { weeklyTrends.sumOf { it.net } }
    val hasWeekActivity = remember(weeklyTrends) { weeklyTrends.any { it.hasActivity || it.amount > 0 } }

    val maxAmount = remember(weeklyTrends) {
        (weeklyTrends.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(40.0)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Label & Selected Day Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY CASH FLOW",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!hasWeekActivity) "No activity this week" else {
                            "Week Net: ${AnalyticsCalculator.formatSignedCurrency(totalWeekNet, currencySymbol, currencyFormatter, showPlus = true)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!hasWeekActivity) MaterialTheme.colorScheme.onSurfaceVariant else if (totalWeekNet >= 0) MaterialTheme.colorScheme.incomeColor else MaterialTheme.colorScheme.spendingColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedDay != null) {
                    val isPositive = selectedDay.isPositive
                    val amount = selectedDay.amount
                    val badgeColor = if (amount == 0.0) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (isPositive) {
                        MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.15f)
                    }
                    val textColor = if (amount == 0.0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else if (isPositive) {
                        MaterialTheme.colorScheme.incomeColor
                    } else {
                        MaterialTheme.colorScheme.spendingColor
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor
                    ) {
                        val badgeAmount = if (amount == 0.0) {
                            "${currencySymbol}0.00"
                        } else {
                            AnalyticsCalculator.formatSignedCurrency(selectedDay.net, currencySymbol, currencyFormatter, showPlus = true)
                        }
                        Text(
                            text = "${selectedDay.fullDayName}: $badgeAmount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 Days Capsule Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyTrends.forEachIndexed { index, dayTrend ->
                    val isSelected = index == selectedDayIndex
                    val hasAmount = dayTrend.amount > 0.0
                    val targetFraction = if (hasAmount) {
                        (dayTrend.amount / maxAmount).toFloat().coerceIn(0.20f, 1f)
                    } else {
                        0f
                    }

                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        label = "barHeight_$index"
                    )

                    val barBrush = if (dayTrend.isPositive) {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.incomeColor,
                                MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.70f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.spendingColor,
                                MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.70f)
                            )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDayIndex = index }
                    ) {
                        // Capsule Track
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(95.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                            RoundedCornerShape(15.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (hasAmount) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            scaleY = animatedFraction
                                            transformOrigin = TransformOrigin(0.5f, 1f)
                                        }
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(barBrush)
                                )
                            } else {
                                // Subtle placeholder resting dot
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day Letter
                        Text(
                            text = dayTrend.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || dayTrend.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (dayTrend.isToday) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Today Indicator Dot
                        if (dayTrend.isToday) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(4.dp))
                        }
                    }
                }
            }

            // Interactive Day Detail Pill Footer
            if (selectedDay != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDay.isToday) "${selectedDay.fullDayName} (Today)" else selectedDay.fullDayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (selectedDay.income > 0 || selectedDay.expense > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (selectedDay.income > 0) {
                                    Text(
                                        text = "+$currencySymbol${currencyFormatter.format(selectedDay.income)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.incomeColor
                                    )
                                }
                                if (selectedDay.expense > 0) {
                                    Text(
                                        text = "-$currencySymbol${currencyFormatter.format(selectedDay.expense)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.spendingColor
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No cash flow on this day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
