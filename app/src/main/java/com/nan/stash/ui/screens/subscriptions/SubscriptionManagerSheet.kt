package com.nan.stash.ui.screens.subscriptions

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    val totalMonthly = subscriptions.filter { it.isActive }.sumOf { it.amount }

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
                AddSubscriptionFormCard(
                    currencySymbol = currencySymbol,
                    onSaveSubscription = { name, amount, category, mode, dueDay ->
                        onAddSubscription(name, amount, category, mode, dueDay)
                        showAddForm = false
                    }
                )
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
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(subscriptions, key = { it.id }) { sub ->
                        val paid = isPaidThisMonth(sub.lastPaidDate)
                        SubscriptionItemCard(
                            sub = sub,
                            currencySymbol = currencySymbol,
                            paid = paid,
                            onLogPayment = onLogPayment,
                            onDelete = onDeleteSubscription
                        )
                    }
                }
            }
        }
    }
}
