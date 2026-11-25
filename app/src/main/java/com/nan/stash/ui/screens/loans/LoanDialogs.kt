package com.nan.stash.ui.screens.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.ui.components.sanitizeDecimalInput
import java.util.Locale

@Composable
fun RepaymentDialog(
    loan: LoanEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, mode: PaymentMode, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", loan.remainingAmount)) }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var noteText by remember { mutableStateOf("") }

    val parsed = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Repayment from ${loan.personName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How much did ${loan.personName} return?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        sanitizeDecimalInput(input)?.let { amountText = it }
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Did they pay in Cash or Online?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPaymentMode == PaymentMode.CASH,
                        onClick = { selectedPaymentMode = PaymentMode.CASH },
                        label = { Text("Cash") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedPaymentMode == PaymentMode.ONLINE,
                        onClick = { selectedPaymentMode = PaymentMode.ONLINE },
                        label = { Text("Online") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsed > 0.0) {
                        onConfirm(parsed, selectedPaymentMode, noteText.trim())
                    }
                },
                enabled = parsed > 0.0
            ) {
                Text("Confirm $currencySymbol$amountText")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMoreLoanDialog(
    loan: LoanEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, mode: PaymentMode, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var noteText by remember { mutableStateOf("") }

    val parsed = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lend More to ${loan.personName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current balance owed: $currencySymbol${String.format(Locale.US, "%.2f", loan.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        sanitizeDecimalInput(input)?.let { amountText = it }
                    },
                    label = { Text("Additional Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Give from Cash or Online?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPaymentMode == PaymentMode.CASH,
                        onClick = { selectedPaymentMode = PaymentMode.CASH },
                        label = { Text("Cash") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedPaymentMode == PaymentMode.ONLINE,
                        onClick = { selectedPaymentMode = PaymentMode.ONLINE },
                        label = { Text("Online") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Note (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsed > 0.0) {
                        onConfirm(parsed, selectedPaymentMode, noteText.trim())
                    }
                },
                enabled = parsed > 0.0
            ) {
                Text("Lend Additional Amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
