package com.example.sugrnote

import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateTimeUtilsTest {

    @Test
    fun testFormatDisplayDateTime_today() {
        val today = LocalDate.now()
        val time = LocalTime.of(13, 17)
        val prefs24 = UserPreferences(is24HourClock = true)
        val result = DateTimeUtils.formatDisplayDateTime(today, time, prefs24)
        assertEquals("Today, 13:17", result)
    }

    @Test
    fun testFormatDisplayDateTime_yesterday() {
        val yesterday = LocalDate.now().minusDays(1)
        val time = LocalTime.of(10, 13)
        val prefs24 = UserPreferences(is24HourClock = true)
        val result = DateTimeUtils.formatDisplayDateTime(yesterday, time, prefs24)
        assertEquals("Yesterday, 10:13", result)
    }

    @Test
    fun testFormatDisplayDateTime_12Hour() {
        val today = LocalDate.now()
        val time = LocalTime.of(13, 17)
        val prefs12 = UserPreferences(is24HourClock = false)
        val result = DateTimeUtils.formatDisplayDateTime(today, time, prefs12)
        assertTrue(result.startsWith("Today, 01:17"))
    }
}
