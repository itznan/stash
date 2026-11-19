package com.nan.stash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor

@Composable
fun BoxScope.FabQuickActionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddMoneyClick: () -> Unit,
    onSpendMoneyClick: () -> Unit,
    onTransferClick: () -> Unit,
    onLendMoneyClick: () -> Unit,
    onSubscriptionsClick: () -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 96.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add Money
                QuickActionRow(
                    title = "Add Money",
                    icon = Icons.Rounded.ArrowUpward,
                    iconBgColor = MaterialTheme.colorScheme.incomeColor,
                    textColor = MaterialTheme.colorScheme.incomeColor,
                    containerColor = MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.15f),
                    onClick = onAddMoneyClick
                )

                // Spend Money
                QuickActionRow(
                    title = "Spend Money",
                    icon = Icons.Rounded.ArrowDownward,
                    iconBgColor = MaterialTheme.colorScheme.spendingColor,
                    textColor = MaterialTheme.colorScheme.spendingColor,
                    containerColor = MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.15f),
                    onClick = onSpendMoneyClick
                )

                // Transfer (Online ⇄ Cash)
                QuickActionRow(
                    title = "Transfer",
                    icon = Icons.Rounded.SwapHoriz,
                    iconBgColor = MaterialTheme.colorScheme.tertiary,
                    textColor = MaterialTheme.colorScheme.tertiary,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    onClick = onTransferClick
                )

                // Lend Money
                QuickActionRow(
                    title = "Lend Money",
                    icon = Icons.Rounded.Person,
                    iconBgColor = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onLendMoneyClick
                )

                // Subscriptions
                QuickActionRow(
                    title = "Subscriptions",
                    icon = Icons.Rounded.EventRepeat,
                    iconBgColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    iconTint = MaterialTheme.colorScheme.onPrimary,
                    onClick = onSubscriptionsClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    textColor: Color,
    containerColor: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
