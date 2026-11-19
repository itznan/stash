package com.nan.stash.ui.screens.loans

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.ui.components.FilledIconButtonSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BorrowerLoanCard(
    loan: LoanEntity,
    currencySymbol: String,
    context: Context,
    onReceivePaymentClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth(),
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
