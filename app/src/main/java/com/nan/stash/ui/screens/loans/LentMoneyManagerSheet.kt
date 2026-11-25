package com.nan.stash.ui.screens.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.model.PaymentMode
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
                        .heightIn(max = 440.dp),
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
