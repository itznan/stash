package com.nan.stash.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LentMoneyManagerSheet(
    loans: List<LoanEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddNewLoanClick: () -> Unit,
    onRecordRepayment: (loan: LoanEntity, amountRepaid: Double, paymentMode: PaymentMode, note: String) -> Unit,
    onAddMoreToLoan: (loan: LoanEntity, additionalAmount: Double, paymentMode: PaymentMode, note: String) -> Unit,
    onDeleteLoan: (loan: LoanEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("Active") } // "Active", "Settled", "All"
    var loanToRepay by remember { mutableStateOf<LoanEntity?>(null) }
    var loanToAddMore by remember { mutableStateOf<LoanEntity?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }

    val activeLoans = remember(loans) { loans.filter { !it.isSettled } }
    val settledLoans = remember(loans) { loans.filter { it.isSettled } }

    val filteredLoans = remember(loans, selectedFilter) {
        when (selectedFilter) {
            "Active" -> activeLoans
            "Settled" -> settledLoans
            else -> loans
        }
    }

    val totalLentActive = remember(activeLoans) { activeLoans.sumOf { it.remainingAmount } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Money Lent Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track loans, repayments, and call contacts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Summary Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL CURRENTLY OWED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.2f", totalLentActive)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Button(
                        onClick = onAddNewLoanClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lend", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Active" to "${activeLoans.size} Active",
                    "Settled" to "${settledLoans.size} Settled",
                    "All" to "All (${loans.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Loans List
            if (filteredLoans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedFilter == "Active") "No active loans! You're all settled." else "No loans found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLoans, key = { it.id }) { loan ->
                        BorrowerLoanCard(
                            loan = loan,
                            currencySymbol = currencySymbol,
                            context = context,
                            onReceivePaymentClick = { loanToRepay = loan },
                            onAddMoreClick = { loanToAddMore = loan },
                            onDeleteClick = { loanToDelete = loan }
                        )
                    }
                }
            }
        }
    }

    // Repayment Dialog
    loanToRepay?.let { loan ->
        RepaymentDialog(
            loan = loan,
            currencySymbol = currencySymbol,
            onDismiss = { loanToRepay = null },
            onConfirm = { amount, mode, note ->
                onRecordRepayment(loan, amount, mode, note)
                loanToRepay = null
            }
        )
    }

    // Add More Loan Dialog
    loanToAddMore?.let { loan ->
        AddMoreLoanDialog(
            loan = loan,
            currencySymbol = currencySymbol,
            onDismiss = { loanToAddMore = null },
            onConfirm = { amount, mode, note ->
                onAddMoreToLoan(loan, amount, mode, note)
                loanToAddMore = null
            }
        )
    }

    // Delete Confirmation Dialog
    loanToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Delete Loan Record?") },
            text = { Text("Are you sure you want to delete the loan record for ${loan.personName}? (Past history transactions will remain safe).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLoan(loan)
                        loanToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BorrowerLoanCard(
    loan: LoanEntity,
    currencySymbol: String,
    context: Context,
    onReceivePaymentClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val lendDateStr = remember(loan.lendDate) { dateFormat.format(Date(loan.lendDate)) }
    val dueDateStr = remember(loan.dueDate) {
        loan.dueDate?.let { dateFormat.format(Date(it)) }
    }

    val isOverdue = remember(loan.dueDate, loan.isSettled) {
        !loan.isSettled && loan.dueDate != null && System.currentTimeMillis() > loan.dueDate
    }

    val progress = remember(loan.totalAmount, loan.remainingAmount) {
        if (loan.totalAmount > 0) {
            ((loan.totalAmount - loan.remainingAmount) / loan.totalAmount).toFloat().coerceIn(0f, 1f)
        } else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Person Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (loan.isSettled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (loan.isSettled) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (loan.isSettled) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = loan.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (loan.phoneNumber.isNotBlank()) {
                            Text(
                                text = loan.phoneNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        loan.isSettled -> MaterialTheme.colorScheme.primaryContainer
                        isOverdue -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Text(
                        text = when {
                            loan.isSettled -> "Settled"
                            isOverdue -> "Overdue"
                            else -> "Active"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            loan.isSettled -> MaterialTheme.colorScheme.onPrimaryContainer
                            isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Remaining Owed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%.2f", loan.remainingAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Total Lent: $currencySymbol${String.format(Locale.US, "%.2f", loan.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reason and Dates
            if (loan.reason.isNotBlank()) {
                Text(
                    text = "Why: \"${loan.reason}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lent on $lendDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (dueDateStr != null) {
                    Text(
                        text = "Due: $dueDateStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Call & WhatsApp Contact Row + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If phone is available, offer direct call and WhatsApp
                if (loan.phoneNumber.isNotBlank()) {
                    val cleanPhone = loan.phoneNumber.replace(" ", "").replace("-", "")

                    // Direct Call Button
                    FilledIconButtonSmall(
                        icon = Icons.Rounded.Call,
                        contentDescription = "Call",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Direct WhatsApp Button
                    FilledIconButtonSmall(
                        icon = Icons.Rounded.Phone,
                        contentDescription = "WhatsApp",
                        containerColor = Color(0xFF25D366).copy(alpha = 0.2f),
                        contentColor = Color(0xFF25D366),
                        onClick = {
                            try {
                                val waNumber = cleanPhone.replace("+", "")
                                val message = "Hi ${loan.personName}, this is a reminder regarding the $currencySymbol${String.format(Locale.US, "%.2f", loan.remainingAmount)} from our loan."
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$waNumber&text=${Uri.encode(message)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // If not settled, offer Receive Payment and + More
                if (!loan.isSettled) {
                    Button(
                        onClick = onReceivePaymentClick,
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Get Money", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAddMoreClick,
                        shape = CircleShape
                    ) {
                        Text("+ More", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Delete Icon
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
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

@Composable
fun FilledIconButtonSmall(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}

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
                        val clean = input.replace(',', '.')
                        if (clean.isEmpty() || clean.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            amountText = clean
                        }
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
                    onValueChange = { amountText = it },
                    label = { Text("Additional Amount ($currencySymbol)") },
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
