package com.example.sugrnote.domain.export

import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExportGenerator {

    fun generate(
        data: ExportReportData,
        prefs: UserPreferences,
        options: ExportSectionOptions = ExportSectionOptions()
    ): String {
        val sb = StringBuilder()
        val zoneId = ZoneId.systemDefault()
        val unitLabel = prefs.glucoseUnit.displayLabel
        val tirLabels = data.tirLabels

        // 1. Meta / Period Summary
        sb.appendLine("SugrNote Glucose Tracking Report")
        sb.appendLine("Report Period,${data.periodStartDate} to ${data.periodEndDate} (${data.windowMonths} Months)")
        sb.appendLine("Generated At,${java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        sb.appendLine("Active Glucose Unit,${prefs.glucoseUnit.name} ($unitLabel)")
        sb.appendLine("Target Glucose Range,${tirLabels.inRangeLabel}")
        sb.appendLine()

        // Overall Metrics
        if (options.includePeriodSummary) {
            sb.appendLine("--- PERIOD SUMMARY ---")
            sb.appendLine("Metric,Value")
            sb.appendLine("Total Readings,${data.overallMetrics.count}")
            sb.appendLine("Average Glucose,${data.overallMetrics.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "N/A"} $unitLabel (${data.overallMetrics.averageMgDl?.let { "%.1f".format(it) } ?: "N/A"} mg/dL)")
            sb.appendLine("Estimated HbA1c (GMI),${data.overallMetrics.estimatedHbA1c?.let { "%.1f%%".format(it) } ?: "N/A"}")
            sb.appendLine("Standard Deviation (SD),${data.overallMetrics.standardDeviation?.let { "%.1f".format(it) } ?: "N/A"} mg/dL")
            sb.appendLine("Coefficient of Variation (% CV),${data.overallMetrics.coefficientOfVariation?.let { "%.1f%%".format(it) } ?: "N/A"}")
            sb.appendLine("Time In Range (${tirLabels.inRangeLabel}),${"%.1f%%".format(data.overallMetrics.inRangePercentage)} (${data.overallMetrics.inRangeCount} readings)")
            sb.appendLine("Time Below Range Low (${tirLabels.lowLabel}),${"%.1f%%".format(data.overallMetrics.targetLowPercentage)} (${data.overallMetrics.lowCount} readings)")
            sb.appendLine("Time Below Range Very Low (${tirLabels.veryLowLabel}),${"%.1f%%".format(data.overallMetrics.veryLowPercentage)} (${data.overallMetrics.veryLowCount} readings)")
            sb.appendLine("Time Above Range High (${tirLabels.highLabel}),${"%.1f%%".format(data.overallMetrics.targetHighPercentage)} (${data.overallMetrics.highCount} readings)")
            sb.appendLine("Time Above Range Very High (${tirLabels.veryHighLabel}),${"%.1f%%".format(data.overallMetrics.veryHighPercentage)} (${data.overallMetrics.veryHighCount} readings)")
            sb.appendLine("Avg Daily Total Insulin,${data.overallInsulinCarb.avgDailyTotalDose?.let { "%.1f u".format(it) } ?: "N/A"}")
            sb.appendLine("Avg Daily Basal (Long-Acting),${data.overallInsulinCarb.avgDailyLongActing?.let { "%.1f u".format(it) } ?: "N/A"}")
            sb.appendLine("Avg Daily Bolus (Short-Acting),${data.overallInsulinCarb.avgDailyShortActing?.let { "%.1f u".format(it) } ?: "N/A"}")
            sb.appendLine("Avg Daily Carbohydrates,${data.overallInsulinCarb.avgDailyCarbs?.let { "%.1f g".format(it) } ?: "N/A"}")
            sb.appendLine()
        }

        // Daily Trend Points
        if (options.includeTrendChart && data.dailyTrendPoints.isNotEmpty()) {
            sb.appendLine("--- DAILY AVERAGE TREND ---")
            sb.appendLine("Date,Daily Average ($unitLabel),Daily Average (mg/dL),Readings Count")
            data.dailyTrendPoints.forEach { point ->
                val avgUnit = GlucoseUnitConverter.format(point.averageMgDl, prefs.glucoseUnit)
                val avgMg = "%.1f".format(point.averageMgDl)
                sb.appendLine("${point.date},$avgUnit,$avgMg,${point.count}")
            }
            sb.appendLine()
        }

        // 2. Monthly Data Summary (if multi-month and selected)
        if (options.includeMonthlySummary && data.isMultiMonth && data.monthlySummaries.isNotEmpty()) {
            sb.appendLine("--- MONTHLY DATA SUMMARY ---")
            sb.appendLine("Month,Readings Count,Average ($unitLabel),Average (mg/dL),In-Range %,Low %,High %,Total Insulin (u),Total Carbs (g)")
            data.monthlySummaries.forEach { m ->
                val avgUnit = m.metrics.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "N/A"
                val avgMg = m.metrics.averageMgDl?.let { "%.1f".format(it) } ?: "N/A"
                val inRange = "%.1f%%".format(m.metrics.inRangePercentage)
                val low = "%.1f%%".format(m.metrics.lowPercentage)
                val high = "%.1f%%".format(m.metrics.highPercentage)
                val totalInsulin = "%.1f".format(m.insulinCarb.totalLongActingUnits + m.insulinCarb.totalShortActingUnits)
                val totalCarbs = "%.1f".format(m.insulinCarb.totalCarbsGrams)
                sb.appendLine("${m.yearMonth},${m.metrics.count},$avgUnit,$avgMg,$inRange,$low,$high,$totalInsulin,$totalCarbs")
            }
            sb.appendLine()
        }

        // 3. Weekly Data Summary (if selected)
        if (options.includeWeeklySummary && data.weeklySummaries.isNotEmpty()) {
            sb.appendLine("--- WEEKLY DATA SUMMARY ---")
            sb.appendLine("Week,Start Date,End Date,Readings Count,Average ($unitLabel),Average (mg/dL),In-Range %,Total Insulin (u),Total Carbs (g)")
            data.weeklySummaries.forEach { w ->
                val avgUnit = w.metrics.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "N/A"
                val avgMg = w.metrics.averageMgDl?.let { "%.1f".format(it) } ?: "N/A"
                val inRange = "%.1f%%".format(w.metrics.inRangePercentage)
                val totalInsulin = "%.1f".format(w.insulinCarb.totalLongActingUnits + w.insulinCarb.totalShortActingUnits)
                val totalCarbs = "%.1f".format(w.insulinCarb.totalCarbsGrams)
                sb.appendLine("Week ${w.weekIndex},${w.startDate},${w.endDate},${w.metrics.count},$avgUnit,$avgMg,$inRange,$totalInsulin,$totalCarbs")
            }
            sb.appendLine()
        }

        // Mealtime Summary (if selected)
        if (options.includeMealtimePatterns && data.mealPeriodAverages.isNotEmpty()) {
            sb.appendLine("--- MEALTIME GLUCOSE PATTERNS ---")
            sb.appendLine("Period,Entries Count,Average ($unitLabel),Average (mg/dL),In-Range %")
            data.mealPeriodAverages.forEach { meal ->
                val mealAvgUnit = GlucoseUnitConverter.format(meal.averageMgDl, prefs.glucoseUnit)
                val mealAvgMg = "%.1f".format(meal.averageMgDl)
                val mealTir = "%.1f%%".format(meal.inRangePercentage)
                sb.appendLine("${meal.period.displayLabel},${meal.count},$mealAvgUnit,$mealAvgMg,$mealTir")
            }
            sb.appendLine()
        }

        // 4. Raw Entries Data Table (if selected)
        if (options.includeLogbook && data.totalEntries.isNotEmpty()) {
            sb.appendLine("--- DETAILED LOGBOOK ENTRIES ---")
            sb.appendLine("Timestamp,Date,Time,Glucose ($unitLabel),Glucose (mg/dL),Status,Period,Insulin Type,Long-Acting (u),Short-Acting (u),Food Logged,Carbs (g),Exercise Timing,Exercise Intensity,Duration (min),Notes")

            data.totalEntries.forEach { entry ->
                val zoned = Instant.ofEpochMilli(entry.dateTime).atZone(zoneId)
                val dateStr = DateTimeUtils.formatDate(zoned, prefs)
                val timeStr = DateTimeUtils.formatTime(zoned, prefs)
                val glucoseUnitStr = GlucoseUnitConverter.format(entry.glucoseMgDl, prefs.glucoseUnit)
                val glucoseMgDlStr = entry.glucoseMgDl.toInt().toString()
                val status = GlucoseStatus.fromValue(entry.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl).name
                val period = entry.period.displayLabel
                val insulinType = entry.insulinType.displayLabel
                val longActing = entry.longActingUnits?.toString() ?: ""
                val shortActing = entry.shortActingUnits?.toString() ?: ""
                val hasFood = if (entry.hasFood) "Yes" else "No"
                val carbs = entry.carbAmount?.toString() ?: ""
                val exerciseTiming = entry.exerciseTiming.name
                val exerciseIntensity = entry.exerciseIntensity ?: ""
                val exerciseDuration = entry.exerciseDuration?.toString() ?: ""
                val notesEscaped = escapeCsv(entry.notes)

                sb.appendLine("${entry.dateTime},$dateStr,$timeStr,$glucoseUnitStr,$glucoseMgDlStr,$status,$period,$insulinType,$longActing,$shortActing,$hasFood,$carbs,$exerciseTiming,$exerciseIntensity,$exerciseDuration,$notesEscaped")
            }
        }

        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        if (text.isEmpty()) return ""
        val needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")
        return if (needsQuotes) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
