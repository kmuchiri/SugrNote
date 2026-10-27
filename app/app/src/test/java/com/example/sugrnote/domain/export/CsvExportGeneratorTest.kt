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

class CsvExportGeneratorTest {

    private val prefs = UserPreferences(
        glucoseUnit = GlucoseUnit.MG_DL,
        lowThresholdMgDl = 70f,
        highThresholdMgDl = 180f
    )

    @Test
    fun generate_includesAllRequiredSectionsAndEscapesNotes() {
        val start = LocalDate.of(2026, 7, 1)
        val end = LocalDate.of(2026, 10, 1)
        val zoneId = ZoneId.systemDefault()

        val entries = listOf(
            GlucoseEntry(
                id = 1,
                glucoseMgDl = 115f,
                dateTime = LocalDate.of(2026, 8, 10).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                period = Period.BEFORE_LUNCH,
                exerciseTiming = ExerciseTiming.NONE,
                insulinType = InsulinType.SHORT_ACTING,
                longActingUnits = null,
                shortActingUnits = 4.5f,
                hasFood = true,
                carbAmount = 45f,
                exerciseIntensity = null,
                exerciseDuration = null,
                sourceType = EntrySource.MANUAL,
                imagePath = null,
                notes = "Had pasta, \"delicious\", with cheese"
            )
        )

        val reportData = ExportDataCalculator.generateReportData(entries, start, end, 3, prefs)
        val csv = CsvExportGenerator.generate(reportData, prefs)

        // Verify sections
        assertTrue(csv.contains("SugrNote Glucose Tracking Report"))
        assertTrue(csv.contains("--- PERIOD SUMMARY ---"))
        assertTrue(csv.contains("--- MONTHLY DATA SUMMARY ---"))
        assertTrue(csv.contains("--- WEEKLY DATA SUMMARY ---"))
        assertTrue(csv.contains("--- DETAILED LOGBOOK ENTRIES ---"))

        // Verify escaping: internal quotes doubled and enclosed in quotes
        assertTrue(csv.contains("\"Had pasta, \"\"delicious\"\", with cheese\""))
        assertTrue(csv.contains("--- DAILY AVERAGE TREND ---"))
    }

    @Test
    fun generate_withMmolL_usesMmolLThresholds() {
        val start = LocalDate.of(2026, 7, 1)
        val end = LocalDate.of(2026, 10, 1)
        val mmolPrefs = prefs.copy(glucoseUnit = GlucoseUnit.MMOL_L)
        val reportData = ExportDataCalculator.generateReportData(emptyList(), start, end, 3, mmolPrefs)
        val csv = CsvExportGenerator.generate(reportData, mmolPrefs)

        assertTrue(csv.contains("Target Glucose Range,3.9–10.0 mmol/L"))
        assertTrue(csv.contains("Time In Range (3.9–10.0 mmol/L)"))
    }

    @Test
    fun generate_omitsUnselectedSections() {
        val start = LocalDate.of(2026, 7, 1)
        val end = LocalDate.of(2026, 10, 1)
        val reportData = ExportDataCalculator.generateReportData(emptyList(), start, end, 3, prefs)
        val options = ExportSectionOptions(
            includePeriodSummary = false,
            includeTrendChart = false,
            includeMonthlySummary = false,
            includeWeeklySummary = false,
            includeMealtimePatterns = false,
            includeLogbook = false
        )
        val csv = CsvExportGenerator.generate(reportData, prefs, options)
        org.junit.Assert.assertFalse(csv.contains("--- PERIOD SUMMARY ---"))
        org.junit.Assert.assertFalse(csv.contains("--- DAILY AVERAGE TREND ---"))
        org.junit.Assert.assertFalse(csv.contains("--- MONTHLY DATA SUMMARY ---"))
        org.junit.Assert.assertFalse(csv.contains("--- WEEKLY DATA SUMMARY ---"))
        org.junit.Assert.assertFalse(csv.contains("--- DETAILED LOGBOOK ENTRIES ---"))
    }
}
