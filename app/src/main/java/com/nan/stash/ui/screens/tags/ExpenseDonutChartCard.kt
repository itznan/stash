package com.nan.stash.ui.screens.tags

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.model.CategorySummary
import com.nan.stash.ui.components.getTagColor
import java.text.NumberFormat

@Composable
fun ExpenseDonutChartCard(
    categorySummaries: List<CategorySummary>,
    totalExpense: Double,
    currencySymbol: String,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EXPENSE DISTRIBUTION",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Donut Chart with center label
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (categorySummaries.isEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                } else {
                    val colorScheme = MaterialTheme.colorScheme
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var currentAngle = -90f
                        categorySummaries.forEach { summary ->
                            val sweep = (summary.percentage / 100f) * 360f
                            val color = getTagColor(summary.tag, colorScheme)
                            drawArc(
                                color = color,
                                startAngle = currentAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                            )
                            currentAngle += sweep
                        }
                    }
                }

                // Center Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencySymbol${currencyFormatter.format(totalExpense)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
