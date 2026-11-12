package com.nan.stash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.components.getTagColor
import com.nan.stash.ui.components.getTagIcon
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor
import com.nan.stash.ui.util.DateUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    innerPadding: PaddingValues,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onClearAll: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf("All", "Income", "Spending", "Transfer", "Lent", "Online", "Cash")

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "Income" -> item.type == TransactionType.INCOME
                "Spending" -> item.type == TransactionType.EXPENSE
                "Transfer" -> item.type == TransactionType.TRANSFER
                "Lent" -> item.type == TransactionType.LEND || item.type == TransactionType.REPAYMENT
                "Online" -> item.paymentMode == PaymentMode.ONLINE
                "Cash" -> item.paymentMode == PaymentMode.CASH
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.tag.contains(searchQuery, ignoreCase = true) ||
                        item.note.contains(searchQuery, ignoreCase = true) ||
                        item.amount.toString().contains(searchQuery)
            }

            matchesFilter && matchesSearch
        }
    }

    // Group transactions by date
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { DateUtils.formatDateKey(it.timestamp) }
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transactions, tags, notes...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape
            )
        }

        // Filter Chips Row + Clear All
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filterOptions, key = { it }) { filter ->
                        val isSelected = selectedFilter == filter
                        val activeColor = when (filter) {
                            "Income" -> MaterialTheme.colorScheme.incomeColor
                            "Spending" -> MaterialTheme.colorScheme.spendingColor
                            else -> MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontWeight = FontWeight.SemiBold) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = activeColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (transactions.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Transactions grouped by day
        if (groupedTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Transactions Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matching transactions will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            groupedTransactions.forEach { (dateKey, itemsInDay) ->
                // Date Header
                item {
                    val dateHeaderLabel = remember(dateKey) {
                        DateUtils.formatDateHeader(itemsInDay.first().timestamp)
                    }
                    val dayNet = remember(itemsInDay) {
                        val inc = itemsInDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val exp = itemsInDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        inc - exp
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateHeaderLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val daySign = if (dayNet >= 0) "+" else "-"
                        val dayColor = if (dayNet >= 0) MaterialTheme.colorScheme.incomeColor else MaterialTheme.colorScheme.spendingColor

                        Text(
                            text = "$daySign$currencySymbol${String.format(Locale.US, "%.2f", Math.abs(dayNet))}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = dayColor
                        )
                    }
                }

                // Transaction cards in that day
                items(
                    items = itemsInDay,
                    key = { it.id },
                    contentType = { "history_item" }
                ) { item ->
                    HistoryItemCard(
                        transaction = item,
                        currencySymbol = currencySymbol,
                        onClick = { onTransactionClick(item) },
                        onDelete = { onDeleteTransaction(item) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Clear Transaction History?") },
            text = { Text("This will permanently remove all transaction entries from your device.") }
        )
    }
}

@Composable
fun HistoryItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tagColor = remember(transaction.tag, colorScheme) { getTagColor(transaction.tag, colorScheme) }
    val tagIcon = remember(transaction.tag) { getTagIcon(transaction.tag) }
    val timeLabel = remember(transaction.timestamp) {
        DateUtils.formatTime(transaction.timestamp)
    }

    Card(
        modifier = Modifier
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = transaction.tag,
                        tint = tagColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                        text = if (transaction.note.isNotBlank()) "${transaction.note} • $modeLabel • $timeLabel" else "$modeLabel • $timeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontSize = 16.sp
                    ),
                    color = amountColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
