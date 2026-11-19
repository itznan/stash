package com.nan.stash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.ui.util.StashHaptics
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagerSheet(
    subscriptions: List<SubscriptionEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddSubscription: (name: String, amount: Double, category: String, mode: PaymentMode, dueDay: Int) -> Unit,
    onDeleteSubscription: (SubscriptionEntity) -> Unit,
    onLogPayment: (SubscriptionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var showAddForm by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Bills") }
    var newMode by remember { mutableStateOf(PaymentMode.ONLINE) }
    var newDueDay by remember { mutableIntStateOf(1) }

    val totalMonthly = subscriptions.filter { it.isActive }.sumOf { it.amount }
    val categories = listOf("Bills", "Entertainment", "Housing", "Utilities", "Fitness", "Cloud", "Other")

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    fun isPaidThisMonth(lastPaid: Long?): Boolean {
        if (lastPaid == null) return false
        val cal = Calendar.getInstance().apply { timeInMillis = lastPaid }
        return cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
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
        ) {
            // Header
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Subscriptions & Bills",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly Commitment: $currencySymbol${String.format(Locale.US, "%.2f", totalMonthly)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add new subscription toggle button
            OutlinedButton(
                onClick = {
                    StashHaptics.keyTap(haptic)
                    showAddForm = !showAddForm
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (showAddForm) Icons.Rounded.Close else Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showAddForm) "Cancel" else "Add New Recurring Bill", fontWeight = FontWeight.SemiBold)
            }

            if (showAddForm) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name (e.g. Netflix, Rent, Gym)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newAmount,
                            onValueChange = { input ->
                                val clean = input.replace(',', '.')
                                if (clean.isEmpty() || clean.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                                    StashHaptics.keyTap(haptic)
                                    newAmount = clean
                                }
                            },
                            label = { Text("Amount") },
                            prefix = { Text("$currencySymbol ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Due Day of Month
                        OutlinedTextField(
                            value = if (newDueDay > 0) newDueDay.toString() else "",
                            onValueChange = { input ->
                                val day = input.toIntOrNull() ?: 0
                                if (day in 0..31) {
                                    newDueDay = day
                                }
                            },
                            label = { Text("Due Day of Month (1 - 31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment mode
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newMode == PaymentMode.ONLINE,
                                onClick = { StashHaptics.keyTap(haptic); newMode = PaymentMode.ONLINE },
                                label = { Text("Online") },
                                leadingIcon = { Icon(Icons.Rounded.AccountBalance, null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = newMode == PaymentMode.CASH,
                                onClick = { StashHaptics.keyTap(haptic); newMode = PaymentMode.CASH },
                                label = { Text("Cash") },
                                leadingIcon = { Icon(Icons.Rounded.Payments, null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val canSave = newName.isNotBlank() && (newAmount.toDoubleOrNull() ?: 0.0) > 0.0 && newDueDay in 1..31
                        Button(
                            onClick = {
                                StashHaptics.buttonClick(haptic)
                                val amount = newAmount.toDoubleOrNull() ?: 0.0
                                onAddSubscription(newName.trim(), amount, newCategory, newMode, newDueDay)
                                newName = ""
                                newAmount = ""
                                showAddForm = false
                            },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Save Subscription", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (subscriptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subscriptions added yet.\nTrack rent, Netflix, gym, utilities and bills!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(subscriptions, key = { it.id }) { sub ->
                        val paid = isPaidThisMonth(sub.lastPaidDate)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paid) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Due on the ${sub.dueDay}${getDaySuffix(sub.dueDay)} • ${sub.paymentMode.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%.2f", sub.amount)} / month",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (paid) {
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Paid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                StashHaptics.buttonClick(haptic)
                                                onLogPayment(sub)
                                            },
                                            shape = CircleShape,
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Log Paid", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            StashHaptics.keyTap(haptic)
                                            onDeleteSubscription(sub)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDaySuffix(day: Int): String {
    return if (day in 11..13) "th"
    else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
