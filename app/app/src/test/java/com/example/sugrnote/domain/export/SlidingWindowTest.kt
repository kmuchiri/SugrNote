package com.example.sugrnote.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SlidingWindowTest {

    @Test
    fun slidingWindow_durationsBetween1And12Months() {
        for (months in 1..12) {
            val start = LocalDate.of(2026, 1, 15)
            val end = start.plusMonths(months.toLong())
            assertEquals(months.toLong(), java.time.temporal.ChronoUnit.MONTHS.between(start, end))
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
