package com.nan.stash.ui.screens.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.model.CategorySummary
import com.nan.stash.data.model.MonthComparison
import com.nan.stash.ui.util.AnalyticsCalculator
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TagsScreen(
    innerPadding: PaddingValues,
    categorySummaries: List<CategorySummary>,
    monthComparison: MonthComparison,
    currencySymbol: String,
    onAddNewTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAnalysisTab by remember { mutableIntStateOf(0) }
    val totalExpense = remember(categorySummaries) {
        AnalyticsCalculator.fromCents(categorySummaries.sumOf { AnalyticsCalculator.toCents(it.amount) })
    }

    val currencyFormatter = remember(currencySymbol) {
        val format = NumberFormat.getNumberInstance(Locale.US)
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        format
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analysis Mode Selector (Categories vs Month-over-Month)
        item {
            val tabs = listOf("Categories", "Month-over-Month")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedAnalysisTab == index,
                        onClick = { selectedAnalysisTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (selectedAnalysisTab == 0) {
            // Top Month Selector Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending by Tag",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Donut Chart Card
            item {
                ExpenseDonutChartCard(
                    categorySummaries = categorySummaries,
                    totalExpense = totalExpense,
                    currencySymbol = currencySymbol,
                    currencyFormatter = currencyFormatter
                )
            }

            // Section Title: Ranked Categories
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANKED CATEGORIES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${categorySummaries.size} tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ranked Tag Items
            if (categorySummaries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No spending records for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(
                    items = categorySummaries,
                    key = { it.tag },
                    contentType = { "category_summary" }
                ) { summary ->
                    RankedCategoryCard(
                        summary = summary,
                        currencySymbol = currencySymbol,
                        formattedAmount = currencyFormatter.format(summary.amount)
                    )
                }
            }

            // Add Tag Button
            item {
                OutlinedButton(
                    onClick = onAddNewTagClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Custom Tag", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Month-over-Month Comparison View
            item {
                MonthOverMonthCard(
                    monthComparison = monthComparison,
                    currencySymbol = currencySymbol,
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}
