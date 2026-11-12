package com.nan.stash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 100% Dynamic Theming Stash Logo
 * Automatically shifts and re-tints in real-time according to Material 3 Dynamic Theming (Monet),
 * adapting to the user's system wallpaper, dynamic accents, and light/dark theme.
 */
@Composable
fun StashLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.28f),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size * 0.62f)) {
                val w = this.size.width
                val h = this.size.height
                val stroke = w * 0.22f
                val corner = stroke * 0.9f
                val cx = w / 2f
                val cy = h / 2f

                // Top Wing (Primary dynamic color -> Secondary dynamic color)
                val topBrush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(0f, 0f),
                    end = Offset(w, cy)
                )

                // Top horizontal bar
                drawRoundRect(
                    brush = topBrush,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, stroke),
                    cornerRadius = CornerRadius(corner, corner)
                )
                // Top-left vertical segment (S curves down the left side)
                drawRoundRect(
                    brush = topBrush,
                    topLeft = Offset(0f, 0f),
                    size = Size(stroke, cy + stroke * 0.25f),
                    cornerRadius = CornerRadius(corner, corner)
                )
                // Center dividing bar (upper-left)
                drawRoundRect(
                    brush = topBrush,
                    topLeft = Offset(0f, cy - stroke * 0.5f),
                    size = Size(cx + stroke * 0.3f, stroke),
                    cornerRadius = CornerRadius(corner, corner)
                )

                // Bottom Wing (Secondary dynamic color -> Primary dynamic color)
                val botBrush = Brush.linearGradient(
                    colors = listOf(secondaryColor, primaryColor),
                    start = Offset(0f, cy),
                    end = Offset(w, h)
                )

                // Bottom horizontal bar
                drawRoundRect(
                    brush = botBrush,
                    topLeft = Offset(0f, h - stroke),
                    size = Size(w, stroke),
                    cornerRadius = CornerRadius(corner, corner)
                )
                // Bottom-right vertical segment (S curves down the right side)
                drawRoundRect(
                    brush = botBrush,
                    topLeft = Offset(w - stroke, cy - stroke * 0.25f),
                    size = Size(stroke, cy + stroke * 0.25f),
                    cornerRadius = CornerRadius(corner, corner)
                )
                // Center dividing bar (lower-right)
                drawRoundRect(
                    brush = botBrush,
                    topLeft = Offset(cx - stroke * 0.3f, cy - stroke * 0.5f),
                    size = Size(cx + stroke * 0.3f, stroke),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
    }
}
