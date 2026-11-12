package com.nan.stash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nan.stash.data.model.PaymentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendMoneyBottomSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmLend: (personName: String, phoneNumber: String, amount: Double, reason: String, dueDate: Long?, paymentMode: PaymentMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var personName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }

    // Quick due date: 0 = none, 7 = 1 week, 14 = 2 weeks, 30 = 1 month
    var dueDaysOption by remember { mutableIntStateOf(0) }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0

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
                Column {
                    Text(
                        text = "Lend Money",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Record money given to someone",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Person Name Field
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("To whom are you giving money? *") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                placeholder = { Text("e.g. John Doe") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input Field with Mobile Keyboard
            OutlinedTextField(
                value = amountString,
                onValueChange = { input ->
                    val clean = input.replace(',', '.')
                    if (clean.isEmpty() || clean.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                        amountString = clean
                    }
                },
                label = { Text("Lent Amount *") },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number Field (for direct Call and WhatsApp)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Contact Phone (For Call & WhatsApp)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                placeholder = { Text("e.g. +1 234 567 8900") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Why he asked money (Reason)
            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it },
                label = { Text("Why did they ask for money? (Optional)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                placeholder = { Text("e.g. Emergency, Rent, Medical") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Given From: Cash vs Online
            Text(
                text = "GIVE MONEY FROM WHICH WALLET?",
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
                    selected = selectedPaymentMode == PaymentMode.CASH,
                    onClick = { selectedPaymentMode = PaymentMode.CASH },
                    label = { Text("Cash Available") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedPaymentMode == PaymentMode.ONLINE,
                    onClick = { selectedPaymentMode = PaymentMode.ONLINE },
                    label = { Text("Online Money") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expected Return Date / Due Time
            Text(
                text = "WHEN SHOULD THEY RETURN MONEY?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "No Rush",
                    7 to "1 Week",
                    14 to "2 Weeks",
                    30 to "1 Month"
                ).forEach { (days, label) ->
                    FilterChip(
                        selected = dueDaysOption == days,
                        onClick = { dueDaysOption = days },
                        label = { Text(label) },
                        leadingIcon = if (dueDaysOption == days && days > 0) {
                            { Icon(imageVector = Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            val canSubmit = parsedAmount > 0.0 && personName.isNotBlank()
            Button(
                onClick = {
                    if (canSubmit) {
                        val dueDate = if (dueDaysOption > 0) {
                            System.currentTimeMillis() + (dueDaysOption * 24L * 60 * 60 * 1000)
                        } else null
                        onConfirmLend(
                            personName.trim(),
                            phoneNumber.trim(),
                            parsedAmount,
                            reasonText.trim(),
                            dueDate,
                            selectedPaymentMode
                        )
                        onDismiss()
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (parsedAmount > 0.0) {
                        "Lend $currencySymbol$amountString to ${if (personName.isNotBlank()) personName.trim() else "Person"}"
                    } else "Lend Money",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
