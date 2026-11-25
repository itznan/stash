package com.nan.stash.ui.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.ui.components.sanitizeDecimalInput
import com.nan.stash.ui.util.StashHaptics

@Composable
fun AddSubscriptionFormCard(
    currencySymbol: String,
    onSaveSubscription: (name: String, amount: Double, category: String, mode: PaymentMode, dueDay: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var newName by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var newMode by remember { mutableStateOf(PaymentMode.ONLINE) }
    var newDueDay by remember { mutableIntStateOf(1) }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    sanitizeDecimalInput(input)?.let {
                        StashHaptics.keyTap(haptic)
                        newAmount = it
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
                    onSaveSubscription(newName.trim(), amount, "Bills", newMode, newDueDay)
                    newName = ""
                    newAmount = ""
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
