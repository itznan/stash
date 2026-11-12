package com.nan.stash.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.DayTrend
import com.nan.stash.ui.components.StashLogo
import com.nan.stash.ui.components.getTagColor
import com.nan.stash.ui.components.getTagIcon
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor
import com.nan.stash.ui.util.DateUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    weeklyTrends: List<DayTrend>,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit = {},
    loans: List<LoanEntity> = emptyList(),
    onLentCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    val onlineBalance = remember(transactions) {
        var bal = 0.0
        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> if (t.paymentMode == PaymentMode.ONLINE) bal += t.amount
                TransactionType.EXPENSE -> if (t.paymentMode == PaymentMode.ONLINE) bal -= t.amount
                TransactionType.TRANSFER -> {
                    if (t.paymentMode == PaymentMode.ONLINE) bal -= t.amount
                    else bal += t.amount
                }
                TransactionType.LEND -> if (t.paymentMode == PaymentMode.ONLINE) bal -= t.amount
                TransactionType.REPAYMENT -> if (t.paymentMode == PaymentMode.ONLINE) bal += t.amount
            }
        }
        bal
    }

    val cashBalance = remember(transactions) {
        var bal = 0.0
        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> if (t.paymentMode == PaymentMode.CASH) bal += t.amount
                TransactionType.EXPENSE -> if (t.paymentMode == PaymentMode.CASH) bal -= t.amount
                TransactionType.TRANSFER -> {
                    if (t.paymentMode == PaymentMode.CASH) bal -= t.amount
                    else bal += t.amount
                }
                TransactionType.LEND -> if (t.paymentMode == PaymentMode.CASH) bal -= t.amount
                TransactionType.REPAYMENT -> if (t.paymentMode == PaymentMode.CASH) bal += t.amount
            }
        }
        bal
    }

    val netBalance = remember(onlineBalance, cashBalance) {
        onlineBalance + cashBalance
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StashLogo(size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Stash",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero Balance Card (28dp Rounded)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL NET BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (transactions.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.incomeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${transactions.size} records",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.incomeColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bold oversized numerals
                    val formattedBalance = currencyFormatter.format(netBalance)
                    Text(
                        text = "$currencySymbol$formattedBalance",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2 Sides: Online Money & Cash Available
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Online Money Side
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalance,
                                            contentDescription = "Online Money",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "Online Money",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val onlineSign = if (onlineBalance < 0) "-" else ""
                                Text(
                                    text = "$onlineSign$currencySymbol${currencyFormatter.format(Math.abs(onlineBalance))}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (onlineBalance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.spendingColor
                                )
                            }
                        }

                        // Cash Available Side
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Payments,
                                            contentDescription = "Cash Available",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "Cash Available",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val cashSign = if (cashBalance < 0) "-" else ""
                                Text(
                                    text = "$cashSign$currencySymbol${currencyFormatter.format(Math.abs(cashBalance))}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (cashBalance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.spendingColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Indicator Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = "Income",
                                        tint = MaterialTheme.colorScheme.incomeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Income",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+$currencySymbol${currencyFormatter.format(totalIncome)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.incomeColor
                                    )
                                }
                            }
                        }

                        // Spending Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = "Spending",
                                        tint = MaterialTheme.colorScheme.spendingColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Spending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "-$currencySymbol${currencyFormatter.format(totalExpense)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.spendingColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lent Money Summary Card
        if (loans.isNotEmpty()) {
            item {
                val totalLentActive = remember(loans) { loans.filter { !it.isSettled }.sumOf { it.remainingAmount } }
                val activeCount = remember(loans) { loans.count { !it.isSettled } }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onLentCardClick),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Payments,
                                    contentDescription = "Lent",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Money Lent (Owed to You)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (activeCount > 0) "$activeCount active borrower${if (activeCount > 1) "s" else ""}" else "All loans settled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currencySymbol${currencyFormatter.format(totalLentActive)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Tap to manage ➔",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Upgraded Interactive Weekly Cash Flow Card
        item {
            WeeklyCashFlowCard(
                weeklyTrends = weeklyTrends,
                currencySymbol = currencySymbol,
                currencyFormatter = currencyFormatter
            )
        }

        // Recent Activity Section
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

@Composable
fun TransactionRowCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateString = remember(transaction.timestamp) {
        DateUtils.formatDate(transaction.timestamp)
    }
    val colorScheme = MaterialTheme.colorScheme
    val tagColor = remember(transaction.tag, colorScheme) { getTagColor(transaction.tag, colorScheme) }
    val tagIcon = remember(transaction.tag) { getTagIcon(transaction.tag) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = transaction.tag,
                        tint = tagColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.tag,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val modeLabel = if (transaction.type == TransactionType.TRANSFER) {
                        if (transaction.paymentMode == PaymentMode.ONLINE) "Online ➔ Cash" else "Cash ➔ Online"
                    } else {
                        if (transaction.paymentMode == PaymentMode.ONLINE) "Online" else "Cash"
                    }
                    Text(
                        text = if (transaction.note.isNotBlank()) "${transaction.note} • $modeLabel • $dateString" else "$modeLabel • $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val sign = when (transaction.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                TransactionType.TRANSFER -> "⇄"
                TransactionType.LEND -> "-"
                TransactionType.REPAYMENT -> "+"
            }
            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> MaterialTheme.colorScheme.incomeColor
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.spendingColor
                TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
                TransactionType.LEND -> MaterialTheme.colorScheme.secondary
                TransactionType.REPAYMENT -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = "$sign$currencySymbol${String.format(Locale.US, "%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = amountColor
            )
        }
    }
}

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
                            val sign = if (totalWeekNet >= 0) "+" else "-"
                            "Week Net: $sign$currencySymbol${currencyFormatter.format(Math.abs(totalWeekNet))}"
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
                        Text(
                            text = "${selectedDay.fullDayName}: ${if (amount == 0.0) currencySymbol + "0.00" else (if (isPositive) "+" else "-") + currencySymbol + currencyFormatter.format(amount)}",
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

                        if (selectedDay.hasActivity) {
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
