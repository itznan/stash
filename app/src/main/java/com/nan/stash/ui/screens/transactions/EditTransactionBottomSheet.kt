package com.nan.stash.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.components.getTagIcon
import com.nan.stash.ui.components.sanitizeDecimalInput
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor
import com.nan.stash.ui.util.DateUtils
import com.nan.stash.ui.util.StashHaptics
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentType by remember { mutableStateOf(transaction.type) }
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", transaction.amount)) }
    var selectedTag by remember { mutableStateOf(transaction.tag) }
    var selectedPaymentMode by remember { mutableStateOf(transaction.paymentMode) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val isLoanTransaction = transaction.type == TransactionType.LEND || transaction.type == TransactionType.REPAYMENT

    val expenseTags = listOf("Food", "Transport", "Bills", "Shopping", "Fun", "Health")
    val incomeTags = listOf("Salary", "Gift", "Freelance", "Other")

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val dateLabel = remember(transaction.timestamp) {
        DateUtils.formatDateTime(transaction.timestamp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoanTransaction) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (transaction.type == TransactionType.LEND) {
                            "Lent Money Record — edit amount in Lent Money Manager"
                        } else {
                            "Loan Repayment Record — edit amount in Lent Money Manager"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else {
                // Transaction Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentType == TransactionType.EXPENSE,
                        onClick = {
                            currentType = TransactionType.EXPENSE
                            if (selectedTag !in expenseTags) {
                                selectedTag = if (transaction.type == TransactionType.EXPENSE) transaction.tag else "Food"
                            }
                        },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.spendingColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = currentType == TransactionType.INCOME,
                        onClick = {
                            currentType = TransactionType.INCOME
                            if (selectedTag !in incomeTags) {
                                selectedTag = if (transaction.type == TransactionType.INCOME) transaction.tag else "Salary"
                            }
                        },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.incomeColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = currentType == TransactionType.TRANSFER,
                        onClick = {
                            currentType = TransactionType.TRANSFER
                            selectedTag = if (selectedPaymentMode == PaymentMode.ONLINE) "Online → Cash" else "Cash → Online"
                        },
                        label = { Text("Transfer") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val clean = sanitizeDecimalInput(input)
                    if (clean != null) {
                        StashHaptics.keyTap(haptic)
                        amountText = clean
                    }
                },
                enabled = !isLoanTransaction,
                label = { Text("Amount ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                prefix = {
                    Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode / Wallet Selector
            Text(
                text = if (currentType == TransactionType.TRANSFER) "TRANSFER DIRECTION" else "WALLET / PAYMENT MODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedPaymentMode == PaymentMode.ONLINE,
                    enabled = !isLoanTransaction,
                    onClick = {
                        selectedPaymentMode = PaymentMode.ONLINE
                        if (currentType == TransactionType.TRANSFER) {
                            selectedTag = "Online → Cash"
                        }
                    },
                    label = {
                        Text(if (currentType == TransactionType.TRANSFER) "Online ➔ Cash" else "Online Money")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedPaymentMode == PaymentMode.CASH,
                    enabled = !isLoanTransaction,
                    onClick = {
                        selectedPaymentMode = PaymentMode.CASH
                        if (currentType == TransactionType.TRANSFER) {
                            selectedTag = "Cash → Online"
                        }
                    },
                    label = {
                        Text(if (currentType == TransactionType.TRANSFER) "Cash ➔ Online" else "Cash Available")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Categories (for Income & Expense)
            if (currentType != TransactionType.TRANSFER && !isLoanTransaction) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val baseTags = if (currentType == TransactionType.INCOME) incomeTags else expenseTags
                val availableTags = remember(currentType, transaction.tag) {
                    if (currentType == transaction.type && baseTags.none { it.equals(transaction.tag, ignoreCase = true) }) {
                        baseTags + transaction.tag
                    } else {
                        baseTags
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag.equals(tag, ignoreCase = true),
                            onClick = { StashHaptics.keyTap(haptic); selectedTag = tag },
                            label = { Text(tag) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getTagIcon(tag),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note Field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    StashHaptics.buttonClick(haptic)
                    if (parsedAmount > 0.0) {
                        val finalTag = if (isLoanTransaction) {
                            transaction.tag
                        } else if (currentType == TransactionType.TRANSFER) {
                            if (selectedPaymentMode == PaymentMode.ONLINE) "Online → Cash" else "Cash → Online"
                        } else selectedTag

                        val updated = transaction.copy(
                            type = if (isLoanTransaction) transaction.type else currentType,
                            amount = if (isLoanTransaction) transaction.amount else parsedAmount,
                            tag = finalTag,
                            note = noteText,
                            paymentMode = if (isLoanTransaction) transaction.paymentMode else selectedPaymentMode
                        )
                        onSave(updated)
                        onDismiss()
                    }
                },
                enabled = parsedAmount > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Transaction", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this transaction?") },
            text = {
                Text(
                    if (isLoanTransaction)
                        "This transaction is linked to Lent Money. Deleting it removes this ledger record but does not change the borrower's loan record."
                    else
                        "Are you sure you want to permanently delete this $currencySymbol$amountText record?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
