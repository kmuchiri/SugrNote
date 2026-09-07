package com.example.sugrnote.domain.util

import com.example.sugrnote.data.settings.UserPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    fun formatTime(time: LocalTime, prefs: UserPreferences): String {
        val pattern = if (prefs.is24HourClock) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatTime(zoned: ZonedDateTime, prefs: UserPreferences): String {
        val pattern = if (prefs.is24HourClock) "HH:mm" else "hh:mm a"
        return zoned.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatDate(date: LocalDate, prefs: UserPreferences): String {
        return date.format(DateTimeFormatter.ofPattern(prefs.dateFormat.pattern))
    }

    fun formatDate(zoned: ZonedDateTime, prefs: UserPreferences): String {
        return zoned.format(DateTimeFormatter.ofPattern(prefs.dateFormat.pattern))
    }

    fun formatDayHeader(zoned: ZonedDateTime, prefs: UserPreferences): String {
        val pattern = "EEE, " + prefs.dateFormat.pattern
        return zoned.format(DateTimeFormatter.ofPattern(pattern))
    }
    
    fun formatDateTime(zoned: ZonedDateTime, prefs: UserPreferences): String {
        return "${formatDate(zoned, prefs)} • ${formatTime(zoned, prefs)}"
    }

    fun formatRelativeDateTime(zoned: ZonedDateTime, prefs: UserPreferences): String {
        val today = LocalDate.now(zoned.zone)
        val entryDate = zoned.toLocalDate()
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(entryDate, today)
        val time = formatTime(zoned, prefs)

        val dayLabel = when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween in 2..6 -> "$daysBetween days ago"
            else -> formatDate(zoned, prefs)
        }
        return "$dayLabel • $time"
    }
}
