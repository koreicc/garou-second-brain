package com.secondbrain.ui.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Converts an ISO 8601 date string to a human-readable relative time string.
 * Examples: "just now", "2 minutes ago", "1 hour ago", "3 days ago", "2 weeks ago"
 *
 * Falls back to the date portion (yyyy-MM-dd) if parsing fails.
 */
fun formatRelativeTime(isoDate: String): String {
    if (isoDate.isBlank()) return ""

    return try {
        val instant: Instant = try {
            // Try parsing as ISO instant (e.g. "2025-01-01T10:00:00Z")
            Instant.parse(isoDate)
        } catch (_: DateTimeParseException) {
            // Try parsing as local date-time with offset (e.g. "2025-01-01T10:00:00+00:00")
            try {
                ZonedDateTime.parse(isoDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
            } catch (_: DateTimeParseException) {
                // Try parsing as local date-time without offset
                try {
                    val localDateTime = java.time.LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                } catch (_: DateTimeParseException) {
                    return isoDate.take(10)
                }
            }
        }

        val now = Instant.now()
        val duration = Duration.between(instant, now)

        // If the date is in the future, show the date
        if (duration.isNegative) {
            return formatDateOnly(isoDate, instant)
        }

        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        when {
            seconds < 60 -> "just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days < 7 -> if (days == 1L) "1 day ago" else "$days days ago"
            weeks < 5 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            months < 12 -> if (months == 1L) "1 month ago" else "$months months ago"
            else -> if (years == 1L) "1 year ago" else "$years years ago"
        }
    } catch (_: Exception) {
        isoDate.take(10)
    }
}

/**
 * Formats a date as a simple date string (yyyy-MM-dd).
 */
private fun formatDateOnly(original: String, instant: Instant): String {
    return try {
        val zdt = instant.atZone(ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        original.take(10)
    }
}
