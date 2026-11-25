package com.nan.stash.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val decimalInputRegex = Regex("""^\d*(\.\d{0,2})?$""")

fun sanitizeDecimalInput(input: String): String? {
    val clean = input.replace(',', '.')
    return if (clean.isEmpty() || decimalInputRegex.matches(clean)) clean else null
}

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
fun FilledIconButtonSmall(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

