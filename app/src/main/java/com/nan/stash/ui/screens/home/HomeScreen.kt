package com.nan.stash.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.DayTrend
import com.nan.stash.ui.util.AnalyticsCalculator
import com.nan.stash.ui.util.WalletBalances
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    transactions: List<TransactionEntity>,
    balances: WalletBalances,
    currencySymbol: String,
    weeklyTrends: List<DayTrend>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTransactionClick: (TransactionEntity) -> Unit = {},
    loans: List<LoanEntity> = emptyList(),
    onLentCardClick: () -> Unit = {},
    subscriptions: List<SubscriptionEntity> = emptyList(),
    onSubscriptionClick: () -> Unit = {},
    lowBalanceWarningEnabled: Boolean = false,
    lowOnlineThreshold: Double = 0.0,
    lowCashThreshold: Double = 0.0
) {

    val isOnlineLow = remember(balances.onlineBalance, lowOnlineThreshold, lowBalanceWarningEnabled) {
        AnalyticsCalculator.isLowBalance(balances.onlineBalance, lowOnlineThreshold, lowBalanceWarningEnabled)
    }

    val isCashLow = remember(balances.cashBalance, lowCashThreshold, lowBalanceWarningEnabled) {
        AnalyticsCalculator.isLowBalance(balances.cashBalance, lowCashThreshold, lowBalanceWarningEnabled)
    }

    val recentTransactions = remember(transactions) {
        transactions.take(5)
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
            bottom = innerPadding.calculateBottomPadding() + 88.dp // Space for bottom bar and FAB
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Greeting Header
        item {
            HomeHeader()
        }

        // Hero Balance Card (28dp Rounded)
        item {
            HeroBalanceCard(
                netBalance = balances.netBalance,
                onlineBalance = balances.onlineBalance,
                cashBalance = balances.cashBalance,
                totalIncome = balances.totalIncome,
                totalExpense = balances.totalExpense,
                transactionCount = transactions.size,
                currencySymbol = currencySymbol,
                currencyFormatter = currencyFormatter,
                isOnlineLow = isOnlineLow,
                isCashLow = isCashLow,
                lowOnlineThreshold = lowOnlineThreshold,
                lowCashThreshold = lowCashThreshold
            )
        }

        // Lent Money Summary Card
        if (loans.isNotEmpty()) {
            item {
                LentMoneySummaryCard(
                    loans = loans,
                    currencySymbol = currencySymbol,
                    currencyFormatter = currencyFormatter,
                    onClick = onLentCardClick
                )
            }
        }

        // Recurring Subscriptions Summary Card
        if (subscriptions.isNotEmpty()) {
            item {
                SubscriptionSummaryCard(
                    subscriptions = subscriptions,
                    currencySymbol = currencySymbol,
                    currencyFormatter = currencyFormatter,
                    onClick = onSubscriptionClick
                )
            }
        }

        // Interactive Weekly Cash Flow Card
        item {
            WeeklyCashFlowCard(
                weeklyTrends = weeklyTrends,
                currencySymbol = currencySymbol,
                currencyFormatter = currencyFormatter
            )
        }

        // Recent Activity Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = onSeeAllClick) {
                    Text("See all", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Tap + to add money or record spending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = recentTransactions,
                key = { it.id },
                contentType = { "transaction" }
            ) { transaction ->
                TransactionRowCard(
                    transaction = transaction,
                    currencySymbol = currencySymbol,
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
    }
}
