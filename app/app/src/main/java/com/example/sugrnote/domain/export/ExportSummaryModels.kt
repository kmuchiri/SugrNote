package com.example.sugrnote.domain.export

import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.domain.model.Period
import java.time.LocalDate
import java.time.YearMonth

/**
 * Summary metrics for a collection of glucose entries.
 */
data class GlucoseMetricSummary(
    val count: Int,
    val averageMgDl: Float?,
    val minMgDl: Float?,
    val maxMgDl: Float?,
    val standardDeviation: Float?,
    val coefficientOfVariation: Float?, // % (SD / Mean * 100)
    val estimatedHbA1c: Float?, // GMI = 3.31 + (0.02392 * mean_mg_dl)
    val veryLowCount: Int,      // < 54 mg/dL (< 3.0 mmol/L)
    val lowCount: Int,          // 54 - 69 mg/dL (3.0 - 3.8 mmol/L)
    val inRangeCount: Int,      // 70 - 180 mg/dL (or user threshold)
    val highCount: Int,         // 181 - 250 mg/dL
    val veryHighCount: Int      // > 250 mg/dL (> 13.9 mmol/L)
) {
    val inRangePercentage: Float
        get() = if (count > 0) (inRangeCount.toFloat() / count) * 100f else 0f

    val lowPercentage: Float
        get() = if (count > 0) ((veryLowCount + lowCount).toFloat() / count) * 100f else 0f

    val veryLowPercentage: Float
        get() = if (count > 0) (veryLowCount.toFloat() / count) * 100f else 0f

    val targetLowPercentage: Float
        get() = if (count > 0) (lowCount.toFloat() / count) * 100f else 0f

    val highPercentage: Float
        get() = if (count > 0) ((highCount + veryHighCount).toFloat() / count) * 100f else 0f

    val targetHighPercentage: Float
        get() = if (count > 0) (highCount.toFloat() / count) * 100f else 0f

    val veryHighPercentage: Float
        get() = if (count > 0) (veryHighCount.toFloat() / count) * 100f else 0f
}

/**
 * Summary of insulin intake and carbohydrates.
 */
data class InsulinAndCarbSummary(
    val totalLongActingUnits: Float,
    val totalShortActingUnits: Float,
    val totalCarbsGrams: Float,
    val activeDaysCount: Int,
    val avgDailyTotalDose: Float?,
    val avgDailyLongActing: Float?,
    val avgDailyShortActing: Float?,
    val avgDailyCarbs: Float?,
    val basalPercentage: Float?,
    val bolusPercentage: Float?
)

/**
 * Meal period breakdown.
 */
data class MealPeriodAverage(
    val period: Period,
    val averageMgDl: Float,
    val count: Int,
    val inRangeCount: Int
) {
    val inRangePercentage: Float
        get() = if (count > 0) (inRangeCount.toFloat() / count) * 100f else 0f
}

/**
 * Weekly breakdown within the chosen period.
 */
data class WeeklySummary(
    val weekIndex: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val metrics: GlucoseMetricSummary,
    val insulinCarb: InsulinAndCarbSummary
)

/**
 * Monthly breakdown within the chosen period.
 */
data class MonthlySummary(
    val yearMonth: YearMonth,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val metrics: GlucoseMetricSummary,
    val insulinCarb: InsulinAndCarbSummary,
    val weeklyBreakdown: List<WeeklySummary>
)

/**
 * Complete consolidated data package for export generation.
 */
data class ExportReportData(
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val windowMonths: Int,
    val totalEntries: List<GlucoseEntry>,
    val overallMetrics: GlucoseMetricSummary,
    val overallInsulinCarb: InsulinAndCarbSummary,
    val mealPeriodAverages: List<MealPeriodAverage>,
    val isMultiMonth: Boolean,
    val monthlySummaries: List<MonthlySummary>,
    val weeklySummaries: List<WeeklySummary>
)
