package com.example.sugrnote.domain.export

import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.EntrySource
import com.example.sugrnote.domain.model.ExerciseTiming
import com.example.sugrnote.domain.model.GlucoseUnit
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExportDataCalculatorTest {

    private val prefs = UserPreferences(
        glucoseUnit = GlucoseUnit.MG_DL,
        lowThresholdMgDl = 70f,
        highThresholdMgDl = 180f
    )

    private val zoneId = ZoneId.systemDefault()

    @Test
    fun computeMetrics_emptyEntries_returnsZeroes() {
        val metrics = ExportDataCalculator.computeMetrics(emptyList(), 70f, 180f)
        assertEquals(0, metrics.count)
        assertEquals(null, metrics.averageMgDl)
        assertEquals(0f, metrics.inRangePercentage, 0.01f)
    }

    @Test
    fun computeMetrics_fiveTierTIR_countsCorrectly() {
        val now = System.currentTimeMillis()
        val entries = listOf(
            createEntry(glucose = 45f, timestamp = now),        // Very Low (<54)
            createEntry(glucose = 65f, timestamp = now + 1000), // Low (54-69)
            createEntry(glucose = 120f, timestamp = now + 2000),// Target (70-180)
            createEntry(glucose = 150f, timestamp = now + 3000),// Target (70-180)
            createEntry(glucose = 200f, timestamp = now + 4000),// High (181-250)
            createEntry(glucose = 280f, timestamp = now + 5000) // Very High (>250)
        )

        val metrics = ExportDataCalculator.computeMetrics(entries, 70f, 180f)
        assertEquals(6, metrics.count)
        assertEquals(1, metrics.veryLowCount)
        assertEquals(1, metrics.lowCount)
        assertEquals(2, metrics.inRangeCount)
        assertEquals(1, metrics.highCount)
        assertEquals(1, metrics.veryHighCount)

        // Target TIR % is 2 / 6 = 33.33%
        assertEquals(33.33f, metrics.inRangePercentage, 0.1f)
        // Est. HbA1c (GMI): 3.31 + 0.02392 * avg
        val avg = (45f + 65f + 120f + 150f + 200f + 280f) / 6f
        assertEquals(avg, metrics.averageMgDl!!, 0.01f)
        val expectedGmi = 3.31f + (0.02392f * avg)
        assertEquals(expectedGmi, metrics.estimatedHbA1c!!, 0.01f)
        assertNotNull(metrics.standardDeviation)
        assertNotNull(metrics.coefficientOfVariation)
    }

    @Test
    fun generateReportData_singleMonth_doesNotProduceMonthlySummaries() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 27)
        val entries = listOf(
            createEntry(glucose = 110f, timestamp = start.atStartOfDay(zoneId).toInstant().toEpochMilli())
        )

        val report = ExportDataCalculator.generateReportData(entries, start, end, 1, prefs)
        assertFalse(report.isMultiMonth)
        assertTrue(report.monthlySummaries.isEmpty())
        assertFalse(report.weeklySummaries.isEmpty())
    }

    @Test
    fun generateReportData_multiMonth_producesMonthlyAndWeeklySummaries() {
        val start = LocalDate.of(2026, 7, 1)
        val end = LocalDate.of(2026, 10, 1)
        val entries = listOf(
            createEntry(glucose = 100f, timestamp = LocalDate.of(2026, 7, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()),
            createEntry(glucose = 120f, timestamp = LocalDate.of(2026, 8, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()),
            createEntry(glucose = 140f, timestamp = LocalDate.of(2026, 9, 15).atStartOfDay(zoneId).toInstant().toEpochMilli())
        )

        val report = ExportDataCalculator.generateReportData(entries, start, end, 3, prefs)
        assertTrue(report.isMultiMonth)
        assertEquals(4, report.monthlySummaries.size) // Jul, Aug, Sep, Oct
        assertEquals(3, report.totalEntries.size)
        assertTrue(report.weeklySummaries.isNotEmpty())
    }

    private fun createEntry(glucose: Float, timestamp: Long): GlucoseEntry {
        return GlucoseEntry(
            id = 0,
            glucoseMgDl = glucose,
            dateTime = timestamp,
            period = Period.FASTING,
            exerciseTiming = ExerciseTiming.NONE,
            insulinType = InsulinType.NONE,
            longActingUnits = null,
            shortActingUnits = null,
            hasFood = false,
            carbAmount = null,
            exerciseIntensity = null,
            exerciseDuration = null,
            sourceType = EntrySource.MANUAL,
            imagePath = null,
            notes = ""
        )
    }
}
