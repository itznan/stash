package com.nan.stash.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zoneId: ZoneId get() = ZoneId.systemDefault()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy • hh:mm a", Locale.US)
    private val pdfDateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.US)
    private val headerFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy", Locale.US)
    private val dateKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    private fun format(timestamp: Long, formatter: DateTimeFormatter, fallback: String = ""): String {
        return try {
            formatter.format(Instant.ofEpochMilli(timestamp).atZone(zoneId))
        } catch (_: Exception) {
            fallback
        }
    }

    fun formatDateKey(timestamp: Long): String = format(timestamp, dateKeyFormatter)

    fun formatDate(timestamp: Long): String = format(timestamp, dateFormatter)

    fun formatShortDate(timestamp: Long): String = format(timestamp, shortDateFormatter)

    fun formatTime(timestamp: Long): String = format(timestamp, timeFormatter)

    fun formatDateTime(timestamp: Long): String = format(timestamp, dateTimeFormatter)

    fun formatPdfDateTime(timestamp: Long): String = format(timestamp, pdfDateTimeFormatter)

    fun formatMonthYear(timestamp: Long): String =
        format(timestamp, monthYearFormatter.withLocale(Locale.getDefault()))

    fun formatFileTimestamp(timestamp: Long = System.currentTimeMillis()): String =
        format(timestamp, fileTimestampFormatter)

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
