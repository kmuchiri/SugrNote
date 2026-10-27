package com.example.sugrnote.domain.export

import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.Period
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object ExportDataCalculator {

    fun generateReportData(
        entries: List<GlucoseEntry>,
        startDate: LocalDate,
        endDate: LocalDate,
        windowMonths: Int,
        prefs: UserPreferences
    ): ExportReportData {
        val lowThresh = prefs.lowThresholdMgDl
        val highThresh = prefs.highThresholdMgDl
        val zoneId = ZoneId.systemDefault()

        val sortedEntries = entries.sortedBy { it.dateTime }

        val overallMetrics = computeMetrics(sortedEntries, lowThresh, highThresh)
        val overallInsulinCarb = computeInsulinAndCarb(sortedEntries, startDate, endDate)
        val mealPeriodAverages = computeMealPeriodAverages(sortedEntries, lowThresh, highThresh)

        val totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
        val isMultiMonth = windowMonths > 1 || totalDays > 31

        val monthlySummaries = if (isMultiMonth) {
            computeMonthlySummaries(sortedEntries, startDate, endDate, lowThresh, highThresh, zoneId)
        } else {
            emptyList()
        }

        val weeklySummaries = computeWeeklySummaries(sortedEntries, startDate, endDate, lowThresh, highThresh, zoneId)

        return ExportReportData(
            periodStartDate = startDate,
            periodEndDate = endDate,
            windowMonths = windowMonths,
            totalEntries = sortedEntries,
            overallMetrics = overallMetrics,
            overallInsulinCarb = overallInsulinCarb,
            mealPeriodAverages = mealPeriodAverages,
            isMultiMonth = isMultiMonth,
            monthlySummaries = monthlySummaries,
            weeklySummaries = weeklySummaries
        )
    }

    fun computeMetrics(
        entries: List<GlucoseEntry>,
        lowThresh: Float,
        highThresh: Float
    ): GlucoseMetricSummary {
        if (entries.isEmpty()) {
            return GlucoseMetricSummary(
                count = 0,
                averageMgDl = null,
                minMgDl = null,
                maxMgDl = null,
                standardDeviation = null,
                coefficientOfVariation = null,
                estimatedHbA1c = null,
                veryLowCount = 0,
                lowCount = 0,
                inRangeCount = 0,
                highCount = 0,
                veryHighCount = 0
            )
        }

        val count = entries.size
        val avg = entries.map { it.glucoseMgDl }.average().toFloat()
        val min = entries.minOf { it.glucoseMgDl }
        val max = entries.maxOf { it.glucoseMgDl }

        // Variance & Standard Deviation
        val variance = entries.map { (it.glucoseMgDl - avg).toDouble().pow(2) }.average()
        val sd = sqrt(variance).toFloat()
        val cv = if (avg > 0f) (sd / avg) * 100f else null

        // GMI / Estimated HbA1c: 3.31 + (0.02392 * mean_mg_dl)
        val gmi = 3.31f + (0.02392f * avg)

        var veryLow = 0
        var low = 0
        var inRange = 0
        var high = 0
        var veryHigh = 0

        entries.forEach { entry ->
            val bg = entry.glucoseMgDl
            if (bg < 54f) {
                veryLow++
            } else if (bg < lowThresh) {
                low++
            } else if (bg <= highThresh) {
                inRange++
            } else if (bg <= 250f) {
                high++
            } else {
                veryHigh++
            }
        }

        return GlucoseMetricSummary(
            count = count,
            averageMgDl = avg,
            minMgDl = min,
            maxMgDl = max,
            standardDeviation = sd,
            coefficientOfVariation = cv,
            estimatedHbA1c = gmi,
            veryLowCount = veryLow,
            lowCount = low,
            inRangeCount = inRange,
            highCount = high,
            veryHighCount = veryHigh
        )
    }

    fun computeInsulinAndCarb(
        entries: List<GlucoseEntry>,
        startDate: LocalDate,
        endDate: LocalDate
    ): InsulinAndCarbSummary {
        val totalLong = entries.mapNotNull { it.longActingUnits }.sum()
        val totalShort = entries.mapNotNull { it.shortActingUnits }.sum()
        val totalCarbs = entries.mapNotNull { it.carbAmount }.sum()
        val totalDose = totalLong + totalShort

        val daysCount = max(1L, ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

        val avgDailyTotal = if (daysCount > 0) totalDose / daysCount else null
        val avgDailyLong = if (daysCount > 0) totalLong / daysCount else null
        val avgDailyShort = if (daysCount > 0) totalShort / daysCount else null
        val avgDailyCarbs = if (daysCount > 0) totalCarbs / daysCount else null

        val basalPct = if (totalDose > 0f) (totalLong / totalDose) * 100f else null
        val bolusPct = if (totalDose > 0f) (totalShort / totalDose) * 100f else null

        return InsulinAndCarbSummary(
            totalLongActingUnits = totalLong,
            totalShortActingUnits = totalShort,
            totalCarbsGrams = totalCarbs,
            activeDaysCount = daysCount,
            avgDailyTotalDose = avgDailyTotal,
            avgDailyLongActing = avgDailyLong,
            avgDailyShortActing = avgDailyShort,
            avgDailyCarbs = avgDailyCarbs,
            basalPercentage = basalPct,
            bolusPercentage = bolusPct
        )
    }

    fun computeMealPeriodAverages(
        entries: List<GlucoseEntry>,
        lowThresh: Float,
        highThresh: Float
    ): List<MealPeriodAverage> {
        return Period.entries.mapNotNull { period ->
            val matching = entries.filter { it.period == period }
            if (matching.isEmpty()) {
                null
            } else {
                val avg = matching.map { it.glucoseMgDl }.average().toFloat()
                val inRange = matching.count { it.glucoseMgDl in lowThresh..highThresh }
                MealPeriodAverage(
                    period = period,
                    averageMgDl = avg,
                    count = matching.size,
                    inRangeCount = inRange
                )
            }
        }
    }

    private fun computeMonthlySummaries(
        entries: List<GlucoseEntry>,
        startDate: LocalDate,
        endDate: LocalDate,
        lowThresh: Float,
        highThresh: Float,
        zoneId: ZoneId
    ): List<MonthlySummary> {
        val startYm = YearMonth.from(startDate)
        val endYm = YearMonth.from(endDate)

        val months = mutableListOf<YearMonth>()
        var curr = startYm
        while (!curr.isAfter(endYm)) {
            months.add(curr)
            curr = curr.plusMonths(1)
        }

        return months.map { ym ->
            val mStart = if (ym == startYm) startDate else ym.atDay(1)
            val mEnd = if (ym == endYm) endDate else ym.atEndOfMonth()

            val monthEntries = entries.filter { entry ->
                val entryDate = Instant.ofEpochMilli(entry.dateTime).atZone(zoneId).toLocalDate()
                !entryDate.isBefore(mStart) && !entryDate.isAfter(mEnd)
            }

            val metrics = computeMetrics(monthEntries, lowThresh, highThresh)
            val insulin = computeInsulinAndCarb(monthEntries, mStart, mEnd)
            val monthWeeks = computeWeeklySummaries(monthEntries, mStart, mEnd, lowThresh, highThresh, zoneId)

            MonthlySummary(
                yearMonth = ym,
                startDate = mStart,
                endDate = mEnd,
                metrics = metrics,
                insulinCarb = insulin,
                weeklyBreakdown = monthWeeks
            )
        }
    }

    fun computeWeeklySummaries(
        entries: List<GlucoseEntry>,
        startDate: LocalDate,
        endDate: LocalDate,
        lowThresh: Float,
        highThresh: Float,
        zoneId: ZoneId
    ): List<WeeklySummary> {
        val summaries = mutableListOf<WeeklySummary>()
        var wStart = startDate
        var weekIndex = 1

        while (!wStart.isAfter(endDate)) {
            val potentialEnd = wStart.plusDays(6)
            val wEnd = if (potentialEnd.isAfter(endDate)) endDate else potentialEnd

            val weekEntries = entries.filter { entry ->
                val entryDate = Instant.ofEpochMilli(entry.dateTime).atZone(zoneId).toLocalDate()
                !entryDate.isBefore(wStart) && !entryDate.isAfter(wEnd)
            }

            val metrics = computeMetrics(weekEntries, lowThresh, highThresh)
            val insulin = computeInsulinAndCarb(weekEntries, wStart, wEnd)

            summaries.add(
                WeeklySummary(
                    weekIndex = weekIndex,
                    startDate = wStart,
                    endDate = wEnd,
                    metrics = metrics,
                    insulinCarb = insulin
                )
            )

            wStart = wStart.plusDays(7)
            weekIndex++
        }

        return summaries
    }
}
