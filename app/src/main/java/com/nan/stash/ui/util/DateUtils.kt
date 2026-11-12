package com.nan.stash.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zoneId: ZoneId get() = ZoneId.systemDefault()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val headerFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy", Locale.US)
    private val dateKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    fun formatDateKey(timestamp: Long): String {
        return try {
            val zdt = Instant.ofEpochMilli(timestamp).atZone(zoneId)
            dateKeyFormatter.format(zdt)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDate(timestamp: Long): String {
        return try {
            val zdt = Instant.ofEpochMilli(timestamp).atZone(zoneId)
            dateFormatter.format(zdt)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatTime(timestamp: Long): String {
        return try {
            val zdt = Instant.ofEpochMilli(timestamp).atZone(zoneId)
            timeFormatter.format(zdt)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDateHeader(timestamp: Long): String {
        return try {
            val targetZdt = Instant.ofEpochMilli(timestamp).atZone(zoneId)
            val targetDate = targetZdt.toLocalDate()
            val today = LocalDate.now(zoneId)

            when {
                targetDate.isEqual(today) -> "TODAY"
                targetDate.isEqual(today.minusDays(1)) -> "YESTERDAY"
                else -> headerFormatter.format(targetZdt).uppercase(Locale.US)
            }
        } catch (_: Exception) {
            "RECENT"
        }
    }
}
