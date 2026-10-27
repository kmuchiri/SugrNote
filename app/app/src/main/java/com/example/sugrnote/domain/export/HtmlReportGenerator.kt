package com.example.sugrnote.domain.export

import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object HtmlReportGenerator {

    fun generate(data: ExportReportData, prefs: UserPreferences): String {
        val zoneId = ZoneId.systemDefault()
        val unitLabel = prefs.glucoseUnit.displayLabel
        val generatedAt = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(data.periodStartDate, data.periodEndDate) + 1
        val periodHeader = "${data.periodStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} — ${data.periodEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} (${data.windowMonths} Mo / $totalDays days)"

        val tir = data.overallMetrics
        val insulin = data.overallInsulinCarb

        val avgGlucoseDisplay = tir.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "—"
        val avgGlucoseMgDl = tir.averageMgDl?.let { "%.1f".format(it) } ?: "—"
        val gmiDisplay = tir.estimatedHbA1c?.let { "%.1f%%".format(it) } ?: "—"
        val sdDisplay = tir.standardDeviation?.let { "%.1f".format(it) } ?: "—"
        val cvDisplay = tir.coefficientOfVariation?.let { "%.1f%%".format(it) } ?: "—"

        val sb = StringBuilder()
        sb.append("""
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>SugrNote Medical Export - ${data.periodStartDate} to ${data.periodEndDate}</title>
<style>
    :root {
        --primary: #1A73E8;
        --primary-bg: #E8F0FE;
        --status-in-range: #2E7D32;
        --status-in-range-bg: #E8F5E9;
        --status-low: #C62828;
        --status-low-bg: #FFEBEE;
        --status-vlow: #880E4F;
        --status-high: #E65100;
        --status-high-bg: #FFF3E0;
        --status-vhigh: #BF360C;
        --text-main: #202124;
        --text-secondary: #5F6368;
        --bg-page: #F8F9FA;
        --bg-card: #FFFFFF;
        --border: #DADCE0;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        background-color: var(--bg-page);
        color: var(--text-main);
        padding: 24px;
        line-height: 1.5;
    }
    .container { max-width: 960px; margin: 0 auto; }
    
    /* Header */
    .report-header {
        background: var(--bg-card);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 24px;
        margin-bottom: 20px;
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        flex-wrap: wrap;
        gap: 16px;
    }
    .brand-title { display: flex; align-items: center; gap: 10px; }
    .brand-title svg { width: 32px; height: 32px; }
    .brand-title h1 { font-size: 24px; font-weight: 700; color: var(--primary); }
    .report-subtitle { color: var(--text-secondary); font-size: 14px; margin-top: 4px; }
    .meta-badge {
        background: var(--primary-bg);
        color: var(--primary);
        padding: 8px 14px;
        border-radius: 20px;
        font-weight: 600;
        font-size: 14px;
    }
    
    /* Cards & Grid */
    .card {
        background: var(--bg-card);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 20px;
        margin-bottom: 20px;
        page-break-inside: avoid;
    }
    .card-title {
        font-size: 18px;
        font-weight: 600;
        color: var(--text-main);
        margin-bottom: 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .grid-4 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 14px;
        margin-bottom: 18px;
    }
    .metric-box {
        background: var(--bg-page);
        border: 1px solid var(--border);
        border-radius: 8px;
        padding: 14px;
    }
    .metric-label { font-size: 12px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
    .metric-value { font-size: 24px; font-weight: 700; color: var(--text-main); margin-top: 4px; }
    .metric-sub { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

    /* Time In Range Bar */
    .tir-container { margin: 16px 0; }
    .tir-bar {
        display: flex;
        height: 28px;
        border-radius: 6px;
        overflow: hidden;
        border: 1px solid var(--border);
    }
    .tir-segment {
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 700;
        color: #FFFFFF;
        white-space: nowrap;
        overflow: hidden;
    }
    .tir-vlow { background: var(--status-vlow); }
    .tir-low { background: var(--status-low); }
    .tir-target { background: var(--status-in-range); }
    .tir-high { background: var(--status-high); }
    .tir-vhigh { background: var(--status-vhigh); }

    .tir-legend {
        display: flex;
        flex-wrap: wrap;
        gap: 14px;
        margin-top: 10px;
        font-size: 12px;
    }
    .legend-item { display: flex; align-items: center; gap: 6px; }
    .legend-dot { width: 10px; height: 10px; border-radius: 2px; }

    /* Tables */
    .table-container { overflow-x: auto; margin-top: 10px; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }
    th {
        background-color: var(--bg-page);
        color: var(--text-secondary);
        font-weight: 600;
        padding: 10px 12px;
        border-bottom: 2px solid var(--border);
    }
    td {
        padding: 10px 12px;
        border-bottom: 1px solid var(--border);
        color: var(--text-main);
    }
    tr:hover { background-color: rgba(26, 115, 232, 0.04); }

    /* Badges */
    .badge {
        display: inline-block;
        padding: 3px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
    }
    .badge-in-range { background: var(--status-in-range-bg); color: var(--status-in-range); }
    .badge-low { background: var(--status-low-bg); color: var(--status-low); }
    .badge-high { background: var(--status-high-bg); color: var(--status-high); }

    /* Print styles */
    @media print {
        body { background: #FFFFFF; padding: 0; }
        .container { max-width: 100%; }
        .card, .report-header { border: 1px solid #CCC; box-shadow: none; margin-bottom: 15px; }
        .card { page-break-inside: avoid; }
        @page { size: A4 portrait; margin: 12mm; }
    }
</style>
</head>
<body>
<div class="container">

    <!-- Header -->
    <header class="report-header">
        <div>
            <div class="brand-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="#1A73E8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
                <h1>SugrNote Medical Summary Report</h1>
            </div>
            <div class="report-subtitle">Personal Ambulatory Glucose & Diabetes Management Profile</div>
            <div style="font-size: 12px; color: var(--text-secondary); margin-top: 6px;">Generated: $generatedAt | Target: ${prefs.lowThresholdMgDl.toInt()}–${prefs.highThresholdMgDl.toInt()} mg/dL</div>
        </div>
        <div class="meta-badge">$periodHeader</div>
    </header>

    <!-- Executive Period Summary -->
    <section class="card">
        <div class="card-title">
            <span>Period Executive Summary</span>
            <span style="font-size: 13px; font-weight: normal; color: var(--text-secondary);">${tir.count} Total Readings</span>
        </div>
        
        <div class="grid-4">
            <div class="metric-box">
                <div class="metric-label">Average Glucose</div>
                <div class="metric-value">$avgGlucoseDisplay <span style="font-size: 14px; font-weight: normal;">$unitLabel</span></div>
                <div class="metric-sub">$avgGlucoseMgDl mg/dL</div>
            </div>
            <div class="metric-box">
                <div class="metric-label">Est. HbA1c (GMI)</div>
                <div class="metric-value">$gmiDisplay</div>
                <div class="metric-sub">Calculated GMI</div>
            </div>
            <div class="metric-box">
                <div class="metric-label">Glycemic Variability</div>
                <div class="metric-value">$cvDisplay</div>
                <div class="metric-sub">SD: $sdDisplay mg/dL (Target: &le;36%)</div>
            </div>
            <div class="metric-box">
                <div class="metric-label">Time In Range (TIR)</div>
                <div class="metric-value" style="color: var(--status-in-range);">${"%.1f%%".format(tir.inRangePercentage)}</div>
                <div class="metric-sub">${tir.inRangeCount} of ${tir.count} readings</div>
            </div>
        </div>

        <!-- 5-Tier Time in Range Stacked Bar -->
        <div class="tir-container">
            <div style="font-size: 13px; font-weight: 600; margin-bottom: 6px;">Time in Range Breakdown (Clinical 5-Tier Targets)</div>
            <div class="tir-bar">
""")

        if (tir.count > 0) {
            val vlowW = tir.veryLowPercentage
            val lowW = tir.targetLowPercentage
            val targetW = tir.inRangePercentage
            val highW = tir.targetHighPercentage
            val vhighW = tir.veryHighPercentage

            if (vlowW > 0) sb.append("""<div class="tir-segment tir-vlow" style="width: ${vlowW}%;">${if (vlowW >= 5f) "%.0f%%".format(vlowW) else ""}</div>""")
            if (lowW > 0) sb.append("""<div class="tir-segment tir-low" style="width: ${lowW}%;">${if (lowW >= 5f) "%.0f%%".format(lowW) else ""}</div>""")
            if (targetW > 0) sb.append("""<div class="tir-segment tir-target" style="width: ${targetW}%;">${if (targetW >= 6f) "%.0f%%".format(targetW) else ""}</div>""")
            if (highW > 0) sb.append("""<div class="tir-segment tir-high" style="width: ${highW}%;">${if (highW >= 5f) "%.0f%%".format(highW) else ""}</div>""")
            if (vhighW > 0) sb.append("""<div class="tir-segment tir-vhigh" style="width: ${vhighW}%;">${if (vhighW >= 5f) "%.0f%%".format(vhighW) else ""}</div>""")
        } else {
            sb.append("""<div class="tir-segment" style="width: 100%; background: #DADCE0; color: #5F6368;">No Data</div>""")
        }

        sb.append("""
            </div>
            <div class="tir-legend">
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-vlow);"></span> Very Low (&lt;54 mg/dL): ${"%.1f%%".format(tir.veryLowPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-low);"></span> Low (54–69 mg/dL): ${"%.1f%%".format(tir.targetLowPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-in-range);"></span> In Range (${prefs.lowThresholdMgDl.toInt()}–${prefs.highThresholdMgDl.toInt()} mg/dL): ${"%.1f%%".format(tir.inRangePercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-high);"></span> High (181–250 mg/dL): ${"%.1f%%".format(tir.targetHighPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-vhigh);"></span> Very High (&gt;250 mg/dL): ${"%.1f%%".format(tir.veryHighPercentage)}</div>
            </div>
        </div>

        <!-- Insulin & Carbs Summary Box -->
        <div style="margin-top: 20px; border-top: 1px solid var(--border); padding-top: 16px;">
            <div style="font-size: 14px; font-weight: 600; margin-bottom: 10px;">Insulin & Nutrition Profile (Daily Averages)</div>
            <div class="grid-4">
                <div class="metric-box">
                    <div class="metric-label">Total Daily Dose (TDD)</div>
                    <div class="metric-value">${insulin.avgDailyTotalDose?.let { "%.1f u".format(it) } ?: "—"}</div>
                    <div class="metric-sub">Basal: ${insulin.basalPercentage?.let { "%.0f%%".format(it) } ?: "—"} | Bolus: ${insulin.bolusPercentage?.let { "%.0f%%".format(it) } ?: "—"}</div>
                </div>
                <div class="metric-box">
                    <div class="metric-label">Daily Basal Insulin</div>
                    <div class="metric-value">${insulin.avgDailyLongActing?.let { "%.1f u".format(it) } ?: "—"}</div>
                    <div class="metric-sub">Long-acting average</div>
                </div>
                <div class="metric-box">
                    <div class="metric-label">Daily Bolus Insulin</div>
                    <div class="metric-value">${insulin.avgDailyShortActing?.let { "%.1f u".format(it) } ?: "—"}</div>
                    <div class="metric-sub">Short-acting average</div>
                </div>
                <div class="metric-box">
                    <div class="metric-label">Daily Carbohydrates</div>
                    <div class="metric-value">${insulin.avgDailyCarbs?.let { "%.0f g".format(it) } ?: "—"}</div>
                    <div class="metric-sub">Average logged intake</div>
                </div>
            </div>
        </div>
    </section>
""")

        // Monthly Summary Section (if > 1 month)
        if (data.isMultiMonth && data.monthlySummaries.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Monthly Data Summary</span>
            <span style="font-size: 13px; font-weight: normal; color: var(--text-secondary);">${data.monthlySummaries.size} Months Breakdown</span>
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Month</th>
                        <th>Readings</th>
                        <th>Average ($unitLabel)</th>
                        <th>Average (mg/dL)</th>
                        <th>Time in Range %</th>
                        <th>Low %</th>
                        <th>High %</th>
                        <th>Daily Insulin</th>
                        <th>Daily Carbs</th>
                    </tr>
                </thead>
                <tbody>
""")
            data.monthlySummaries.forEach { m ->
                val monthLabel = m.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                val mAvgUnit = m.metrics.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "—"
                val mAvgMg = m.metrics.averageMgDl?.let { "%.1f".format(it) } ?: "—"
                val mInRange = "%.1f%%".format(m.metrics.inRangePercentage)
                val mLow = "%.1f%%".format(m.metrics.lowPercentage)
                val mHigh = "%.1f%%".format(m.metrics.highPercentage)
                val mInsulin = m.insulinCarb.avgDailyTotalDose?.let { "%.1f u".format(it) } ?: "—"
                val mCarbs = m.insulinCarb.avgDailyCarbs?.let { "%.0f g".format(it) } ?: "—"

                sb.append("""
                    <tr>
                        <td><strong>$monthLabel</strong></td>
                        <td>${m.metrics.count}</td>
                        <td><strong>$mAvgUnit</strong></td>
                        <td>$mAvgMg</td>
                        <td><span class="badge badge-in-range">$mInRange</span></td>
                        <td><span class="badge badge-low">$mLow</span></td>
                        <td><span class="badge badge-high">$mHigh</span></td>
                        <td>$mInsulin</td>
                        <td>$mCarbs</td>
                    </tr>
""")
            }
            sb.append("""
                </tbody>
            </table>
        </div>
    </section>
""")
        }

        // Weekly Summary Section
        if (data.weeklySummaries.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Weekly Data Summary</span>
            <span style="font-size: 13px; font-weight: normal; color: var(--text-secondary);">${data.weeklySummaries.size} Weeks in Window</span>
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Week</th>
                        <th>Interval</th>
                        <th>Readings</th>
                        <th>Average ($unitLabel)</th>
                        <th>Average (mg/dL)</th>
                        <th>In Range %</th>
                        <th>Total Insulin</th>
                        <th>Total Carbs</th>
                    </tr>
                </thead>
                <tbody>
""")
            data.weeklySummaries.forEach { w ->
                val intervalStr = "${w.startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} – ${w.endDate.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                val wAvgUnit = w.metrics.averageMgDl?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "—"
                val wAvgMg = w.metrics.averageMgDl?.let { "%.1f".format(it) } ?: "—"
                val wInRange = "%.1f%%".format(w.metrics.inRangePercentage)
                val wInsulin = "%.1f u".format(w.insulinCarb.totalLongActingUnits + w.insulinCarb.totalShortActingUnits)
                val wCarbs = "%.0f g".format(w.insulinCarb.totalCarbsGrams)

                sb.append("""
                    <tr>
                        <td><strong>Week ${w.weekIndex}</strong></td>
                        <td>$intervalStr</td>
                        <td>${w.metrics.count}</td>
                        <td><strong>$wAvgUnit</strong></td>
                        <td>$wAvgMg</td>
                        <td><span class="badge badge-in-range">$wInRange</span></td>
                        <td>$wInsulin</td>
                        <td>$wCarbs</td>
                    </tr>
""")
            }
            sb.append("""
                </tbody>
            </table>
        </div>
    </section>
""")
        }

        // Mealtime Summary Section
        if (data.mealPeriodAverages.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">Mealtime Glucose Patterns</div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Period</th>
                        <th>Entries</th>
                        <th>Average ($unitLabel)</th>
                        <th>Average (mg/dL)</th>
                        <th>In Range %</th>
                    </tr>
                </thead>
                <tbody>
""")
            data.mealPeriodAverages.forEach { meal ->
                val mealAvgUnit = GlucoseUnitConverter.format(meal.averageMgDl, prefs.glucoseUnit)
                val mealAvgMg = "%.1f".format(meal.averageMgDl)
                val mealTir = "%.1f%%".format(meal.inRangePercentage)
                sb.append("""
                    <tr>
                        <td><strong>${meal.period.displayLabel}</strong></td>
                        <td>${meal.count}</td>
                        <td><strong>$mealAvgUnit</strong></td>
                        <td>$mealAvgMg</td>
                        <td><span class="badge badge-in-range">$mealTir</span></td>
                    </tr>
""")
            }
            sb.append("""
                </tbody>
            </table>
        </div>
    </section>
""")
        }

        // Detailed Logbook Section
        sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Detailed Logbook</span>
            <span style="font-size: 13px; font-weight: normal; color: var(--text-secondary);">${data.totalEntries.size} Recorded Entries</span>
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Date & Time</th>
                        <th>Glucose</th>
                        <th>Period</th>
                        <th>Insulin</th>
                        <th>Carbs</th>
                        <th>Exercise</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
""")
        data.totalEntries.reversed().forEach { entry ->
            val zoned = Instant.ofEpochMilli(entry.dateTime).atZone(zoneId)
            val dateStr = DateTimeUtils.formatDate(zoned, prefs)
            val timeStr = DateTimeUtils.formatTime(zoned, prefs)
            val glucoseDisplay = GlucoseUnitConverter.format(entry.glucoseMgDl, prefs.glucoseUnit)
            val status = GlucoseStatus.fromValue(entry.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl)
            val badgeClass = when (status) {
                GlucoseStatus.IN_RANGE -> "badge-in-range"
                GlucoseStatus.LOW -> "badge-low"
                GlucoseStatus.HIGH -> "badge-high"
            }

            val insulinParts = mutableListOf<String>()
            entry.longActingUnits?.let { insulinParts.add("${it}u Basal") }
            entry.shortActingUnits?.let { insulinParts.add("${it}u Bolus") }
            val insulinText = if (insulinParts.isEmpty()) "—" else insulinParts.joinToString(", ")
            val carbsText = entry.carbAmount?.let { "${it}g" } ?: "—"
            val exerciseText = if (entry.exerciseTiming != com.example.sugrnote.domain.model.ExerciseTiming.NONE) {
                "${entry.exerciseTiming.name}${entry.exerciseDuration?.let { " (${it}m)" } ?: ""}"
            } else {
                "—"
            }
            val notesText = if (entry.notes.isBlank()) "—" else escapeHtml(entry.notes)

            sb.append("""
                <tr>
                    <td>$dateStr <span style="color: var(--text-secondary); font-size: 11px;">$timeStr</span></td>
                    <td><span class="badge $badgeClass">$glucoseDisplay $unitLabel</span></td>
                    <td>${entry.period.displayLabel}</td>
                    <td>$insulinText</td>
                    <td>$carbsText</td>
                    <td>$exerciseText</td>
                    <td>$notesText</td>
                </tr>
""")
        }

        sb.append("""
                </tbody>
            </table>
        </div>
    </section>

    <!-- Footer -->
    <footer style="text-align: center; color: var(--text-secondary); font-size: 12px; margin-top: 30px; padding-bottom: 20px;">
        Generated offline on-device by SugrNote. All health records remain private to your phone.
    </footer>

</div>
</body>
</html>
""")

        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
