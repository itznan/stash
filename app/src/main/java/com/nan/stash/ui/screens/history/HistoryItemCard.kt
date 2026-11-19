package com.nan.stash.ui.screens.history

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

@Composable
fun HistoryItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tagColor = remember(transaction.tag, colorScheme) { getTagColor(transaction.tag, colorScheme) }
    val tagIcon = remember(transaction.tag) { getTagIcon(transaction.tag) }
    val timeLabel = remember(transaction.timestamp) {
        DateUtils.formatTime(transaction.timestamp)
    }

    Card(
        modifier = modifier
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
