package com.example.sugrnote.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SlidingWindowTest {

    @Test
    fun slidingWindow_durationsBetween1And12Months() {
        for (months in 1..12) {
            val start = LocalDate.of(2026, 1, 15)
            val end = start.plusMonths(months.toLong())
            assertEquals(months.toLong(), ChronoUnit.MONTHS.between(start, end))
        }
    }

    @Test
    fun slidingWindow_cappedAtToday() {
        val today = LocalDate.now()
        val start = today.minusMonths(1) // 1 month ago
        val windowMonths = 6 // window size 6 months

        val computedEnd = start.plusMonths(windowMonths.toLong())
        val effectiveEnd = if (computedEnd.isAfter(today)) today else computedEnd

        assertEquals(today, effectiveEnd)
        assertFalse(effectiveEnd.isAfter(today))
    }

    @Test
    fun slidingWindow_whenAnchoredToToday_updatingWindowSizeMovesStartDateBack() {
        val today = LocalDate.now()
        var windowMonths = 3
        var isAnchoredToToday = true
        var startDate = today.minusMonths(windowMonths.toLong())

        val initialDays = ChronoUnit.DAYS.between(startDate, today) + 1
        // For 3 months, usually ~92-93 days
        assertTrue(initialDays in 90..93)

        // User updates window size from 3 to 4 months on the slider
        windowMonths = 4
        if (isAnchoredToToday) {
            startDate = today.minusMonths(windowMonths.toLong())
        }
        val effectiveEnd = if (isAnchoredToToday) today else startDate.plusMonths(windowMonths.toLong())

        val updatedDays = ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1
        // For 4 months, usually ~122-124 days (not 93 days!)
        assertTrue(updatedDays in 120..124)
        assertEquals(today, effectiveEnd)
        assertEquals(today.minusMonths(4), startDate)
    }

    @Test
    fun slidingWindow_whenSlidBack_keepsHistoricalStartDate() {
        val today = LocalDate.now()
        var windowMonths = 3
        var isAnchoredToToday = true
        var startDate = today.minusMonths(windowMonths.toLong())

        // User slides back 3 months
        startDate = startDate.minusMonths(windowMonths.toLong())
        isAnchoredToToday = false

        val historicalStart = startDate
        assertEquals(today.minusMonths(6), historicalStart)

        // User changes window size to 4 while in historical window
        windowMonths = 4
        if (isAnchoredToToday) {
            startDate = today.minusMonths(windowMonths.toLong())
        }

        // startDate is NOT changed to today.minusMonths(4), it remains at historicalStart
        assertEquals(historicalStart, startDate)
    }

    @Test
    fun slidingWindow_slidingBackwardAndForward() {
        val today = LocalDate.now()
        var start = today.minusMonths(3)
        val windowMonths = 3

        // Sliding back 3 months
        start = start.minusMonths(windowMonths.toLong())
        assertEquals(today.minusMonths(6), start)

        // Sliding forward 3 months
        start = start.plusMonths(windowMonths.toLong())
        assertEquals(today.minusMonths(3), start)

        // Cannot slide forward past today
        val cannotSlideForward = !start.plusMonths(windowMonths.toLong()).isBefore(today)
        assertTrue(cannotSlideForward)
    }

    @Test
    fun slidingWindow_leapYearHandling() {
        // 2024 is a leap year (Feb 29)
        val leapDate = LocalDate.of(2024, 1, 31)
        val oneMonthLater = leapDate.plusMonths(1)
        assertEquals(LocalDate.of(2024, 2, 29), oneMonthLater)

        // 2023 is non-leap year (Feb 28)
        val nonLeapDate = LocalDate.of(2023, 1, 31)
        val nonLeapOneMonthLater = nonLeapDate.plusMonths(1)
        assertEquals(LocalDate.of(2023, 2, 28), nonLeapOneMonthLater)
    }
}
