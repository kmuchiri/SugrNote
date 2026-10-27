package com.example.sugrnote.domain.export

import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.EntrySource
import com.example.sugrnote.domain.model.ExerciseTiming
import com.example.sugrnote.domain.model.GlucoseUnit
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HtmlReportGeneratorTest {

    private val prefs = UserPreferences(
        glucoseUnit = GlucoseUnit.MG_DL,
        lowThresholdMgDl = 70f,
        highThresholdMgDl = 180f
    )

    @Test
    fun generate_producesValidSelfContainedHtmlWithSections() {
        val start = LocalDate.of(2026, 7, 1)
        val end = LocalDate.of(2026, 10, 1)
        val zoneId = ZoneId.systemDefault()

        val entries = listOf(
            GlucoseEntry(
                id = 1,
                glucoseMgDl = 125f,
                dateTime = LocalDate.of(2026, 8, 12).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                period = Period.FASTING,
                exerciseTiming = ExerciseTiming.NONE,
                insulinType = InsulinType.BOTH,
                longActingUnits = 14f,
                shortActingUnits = 3f,
                hasFood = true,
                carbAmount = 30f,
                exerciseIntensity = null,
                exerciseDuration = null,
                sourceType = EntrySource.MANUAL,
                imagePath = null,
                notes = "Fasting BG test & healthy salad <lunch>"
            )
        )

        val reportData = ExportDataCalculator.generateReportData(entries, start, end, 3, prefs)
        val html = HtmlReportGenerator.generate(reportData, prefs)

        // Valid HTML structure
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("</html>"))

        // Contains styling and print styles
        assertTrue(html.contains("@media print"))

        // Contains executive summary, monthly breakdown, weekly breakdown, and logbook
        assertTrue(html.contains("SugrNote Medical Summary Report"))
        assertTrue(html.contains("Period Executive Summary"))
        assertTrue(html.contains("Monthly Data Summary"))
        assertTrue(html.contains("Weekly Data Summary"))
        assertTrue(html.contains("Detailed Logbook"))

        // Checks HTML character escaping
        assertTrue(html.contains("&lt;lunch&gt;"))
        assertTrue(html.contains("Fasting BG test &amp; healthy salad"))
    }
}
