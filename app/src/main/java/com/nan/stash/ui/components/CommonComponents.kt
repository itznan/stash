package com.nan.stash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.unit.sp

fun getTagIcon(tag: String): ImageVector {
    val lower = tag.lowercase()
    return when {
        lower == "food" -> Icons.Rounded.Restaurant
        lower == "shopping" -> Icons.Rounded.ShoppingBag
        lower == "bills" -> Icons.Rounded.Receipt
        lower == "transport" -> Icons.Rounded.DirectionsCar
        lower == "fun" -> Icons.Rounded.SportsEsports
        lower == "health" -> Icons.Rounded.MedicalServices
        lower == "salary" -> Icons.Rounded.AccountBalance
        lower == "gift" -> Icons.Rounded.CardGiftcard
        lower == "freelance" -> Icons.Rounded.LaptopMac
        lower in listOf("transfer", "online → cash", "cash → online", "atm", "deposit") -> Icons.Rounded.SwapHoriz
        lower.startsWith("lent") || lower.startsWith("loan") -> Icons.Rounded.Person
        lower.startsWith("repaid") || lower.startsWith("repayment") -> Icons.Rounded.Payments
        else -> Icons.Rounded.Category
    }
}

// 100% Dynamic Colors strictly derived from Material 3 dynamic colorScheme
fun getTagColor(tag: String, colorScheme: ColorScheme): Color {
    val lower = tag.lowercase()
    return when {
        lower == "food" -> colorScheme.primary
        lower == "shopping" -> colorScheme.secondary
        lower == "bills" -> colorScheme.error
        lower == "transport" -> colorScheme.tertiary
        lower == "fun" -> colorScheme.tertiaryContainer
        lower == "health" -> colorScheme.errorContainer
        lower in listOf("salary", "freelance") -> colorScheme.primary
        lower == "gift" -> colorScheme.secondaryContainer
        lower in listOf("transfer", "online → cash", "cash → online", "atm", "deposit") -> colorScheme.tertiary
        lower.startsWith("lent") || lower.startsWith("loan") -> colorScheme.secondary
        lower.startsWith("repaid") || lower.startsWith("repayment") -> colorScheme.primary
        else -> colorScheme.surfaceVariant
    }
}

@Composable
fun getDynamicTagColor(tag: String): Color = getTagColor(tag, MaterialTheme.colorScheme)

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                if (key == "DEL") onDeleteClick() else onDigitClick(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "DEL") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
