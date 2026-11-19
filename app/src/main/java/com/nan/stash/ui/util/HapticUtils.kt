package com.nan.stash.ui.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object StashHaptics {
    fun keyTap(haptic: HapticFeedback) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    fun buttonClick(haptic: HapticFeedback) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }
}
