package com.example.sugrnote.domain.export

import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.model.GlucoseUnit
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object HtmlReportGenerator {

    fun generate(
        data: ExportReportData,
        prefs: UserPreferences,
        options: ExportSectionOptions = ExportSectionOptions()
    ): String {
        val zoneId = ZoneId.systemDefault()
        val unitLabel = prefs.glucoseUnit.displayLabel
        val generatedAt = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(data.periodStartDate, data.periodEndDate) + 1
        val periodHeader = "${data.periodStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} — ${data.periodEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} (${data.windowMonths} Mo / $totalDays days)"

        val tir = data.overallMetrics
        val insulin = data.overallInsulinCarb
        val tirLabels = data.tirLabels

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
<title>SugrNote Glucose Tracking Report - ${data.periodStartDate} to ${data.periodEndDate}</title>
<style>
    @font-face {
        font-family: 'Baflion';
        src: url('data:font/otf;base64,${EmbeddedFont.BAFLION_BASE64}') format('opentype');
        font-weight: normal;
        font-style: normal;
    }
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
        padding: 20px;
        line-height: 1.45;
    }
    .container { max-width: 960px; margin: 0 auto; }
    
    /* Header */
    .report-header {
        background: var(--bg-card);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 16px 20px;
        margin-bottom: 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;
    }
    .header-left {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    .brand-title {
        font-family: 'Baflion', -apple-system, sans-serif;
        font-size: 26px;
        font-weight: 700;
        color: var(--primary);
        line-height: 1.1;
        letter-spacing: 0.5px;
    }
    .report-title {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-main);
        margin-top: 2px;
    }
    .header-right {
        text-align: right;
        font-size: 12px;
        color: var(--text-secondary);
        display: flex;
        flex-direction: column;
        gap: 3px;
    }
    .meta-label {
        font-weight: 600;
        color: var(--text-main);
    }
    
    /* Cards & Grid */
    .card {
        background: var(--bg-card);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 16px 20px;
        margin-bottom: 16px;
    }
    .card-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-main);
        margin-bottom: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .grid-4 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
        gap: 10px;
        margin-bottom: 14px;
    }
    .metric-box {
        background: var(--bg-page);
        border: 1px solid var(--border);
        border-radius: 8px;
        padding: 10px 12px;
    }
    .metric-label { font-size: 11px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
    .metric-value { font-size: 22px; font-weight: 700; color: var(--text-main); margin-top: 2px; }
    .metric-sub { font-size: 11px; color: var(--text-secondary); margin-top: 1px; }

    /* Time In Range Bar */
    .tir-container { margin: 12px 0; }
    .tir-bar {
        display: flex;
        height: 24px;
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
        gap: 12px;
        margin-top: 8px;
        font-size: 11px;
    }
    .legend-item { display: flex; align-items: center; gap: 5px; }
    .legend-dot { width: 9px; height: 9px; border-radius: 2px; }

    /* Trend Chart */
    .trend-chart-box {
        margin-top: 14px;
        padding-top: 12px;
        border-top: 1px solid var(--border);
    }
    .chart-container {
        width: 100%;
        height: 190px;
        margin: 8px 0 4px 0;
    }

    /* Tables */
    .table-container { overflow-x: auto; margin-top: 8px; }
    table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
    th {
        background-color: var(--bg-page);
        color: var(--text-secondary);
        font-weight: 600;
        padding: 8px 10px;
        border-bottom: 2px solid var(--border);
    }
    td {
        padding: 8px 10px;
        border-bottom: 1px solid var(--border);
        color: var(--text-main);
    }
    tr:hover { background-color: rgba(26, 115, 232, 0.04); }

    /* Badges */
    .badge {
        display: inline-block;
        padding: 2px 7px;
        border-radius: 4px;
        font-size: 11px;
        font-weight: 600;
    }
    .badge-in-range { background: var(--status-in-range-bg); color: var(--status-in-range); }
    .badge-low { background: var(--status-low-bg); color: var(--status-low); }
    .badge-high { background: var(--status-high-bg); color: var(--status-high); }

    /* Print styles: Ensure executive summary and graph fit on page 1 without being pushed away! */
    @media print {
        body { background: #FFFFFF; padding: 0; }
        .container { max-width: 100%; width: 100%; margin: 0; padding: 0; }
        .report-header {
            border: 1px solid #DADCE0;
            box-shadow: none;
            padding: 10px 14px;
            margin-bottom: 8px;
            page-break-inside: avoid;
        }
        .brand-title { font-size: 22px; }
        .report-title { font-size: 13px; }
        .card {
            border: 1px solid #DADCE0;
            box-shadow: none;
            padding: 12px 14px;
            margin-bottom: 10px;
            page-break-inside: auto; /* Allows summary and graph to start immediately on page 1! */
        }
        .metric-box {
            padding: 6px 8px;
            page-break-inside: avoid;
        }
        .metric-value { font-size: 18px; }
        .grid-4 { gap: 8px; margin-bottom: 8px; }
        .tir-container { margin: 8px 0; page-break-inside: avoid; }
        .tir-bar { height: 20px; }
        .trend-chart-box {
            margin-top: 8px;
            padding-top: 8px;
            page-break-inside: avoid;
        }
        .chart-container { height: 160px; }
        .insulin-summary-box { page-break-inside: avoid; }
        table { page-break-inside: auto; }
        tr { page-break-inside: avoid; page-break-after: auto; }
        thead { display: table-header-group; }
        @page { size: A4 portrait; margin: 8mm 10mm; }
    }
</style>
</head>
<body>
<div class="container">

    <!-- Header -->
    <header class="report-header">
        <div class="header-left">
            <!-- SugrNote Official Droplet / S Logo -->
            <svg viewBox="0 0 53 53" width="38" height="38" xmlns="http://www.w3.org/2000/svg">
                <rect width="53" height="53" rx="12" fill="#1A73E8"/>
                <g transform="translate(7.4, 7.4) scale(0.72)">
                    <path d="m26.432,8.698 l-0.651,1.508c-0.954,2.209 -3.332,5.8 -6.766,10.214 -5.467,7.028 -6.861,9.617 -7.036,13.063 -0.086,1.69 -0.029,2.191 0.363,3.179 1.49,3.761 5.958,6.7 11.313,7.442 1.807,0.25 5.404,0.077 7.196,-0.346 6.945,-1.642 10.916,-6.132 9.984,-11.29 -0.566,-3.136 -2.238,-5.974 -7.414,-12.59 -3.14,-4.013 -5.144,-7.067 -6.236,-9.499zM26.996,23.301c2.508,0 6.663,0.358 7.989,0.788 0,0 2.219,3.004 3.191,4.739 0.334,0.596 0.696,1.28 0.696,1.28 -1.755,-0.752 -10.227,-0.967 -11.768,-0.967 -1.29,0 -1.863,0.143 -1.863,0.537 0,1.254 9.601,-0.287 9.601,5.911 0,4.299 -3.009,5.983 -9.601,5.983 -2.472,0 -5.803,-0.179 -7.165,-0.573 0,0 -1.508,-0.808 -2.931,-2.53 -1.811,-2.191 -1.308,-3.708 -1.308,-3.708 1.648,0.573 8.68,1.008 11.26,1.008 1.684,0 2.436,-0.179 2.436,-0.609 0,-1.254 -9.458,0.394 -9.458,-5.911 0,-4.084 3.117,-5.946 8.92,-5.946z" fill="#FFFFFF"/>
                </g>
            </svg>
            <div>
                <h1 class="brand-title">SugrNote</h1>
                <div class="report-title">Glucose Tracking Report</div>
            </div>
        </div>
        <div class="header-right">
            <div><span class="meta-label">Generated:</span> $generatedAt</div>
            <div><span class="meta-label">Time Frame:</span> $periodHeader</div>
        </div>
    </header>
""")

        // Period Executive Summary
        if (options.includePeriodSummary) {
            sb.append("""
    <!-- Executive Period Summary -->
    <section class="card">
        <div class="card-title">
            <span>Period Executive Summary</span>
            <span style="font-size: 12px; font-weight: normal; color: var(--text-secondary);">${tir.count} Total Readings</span>
        </div>
        
        <div class="grid-4">
            <div class="metric-box">
                <div class="metric-label">Average Glucose</div>
                <div class="metric-value">$avgGlucoseDisplay <span style="font-size: 13px; font-weight: normal;">$unitLabel</span></div>
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
            <div style="font-size: 12px; font-weight: 600; margin-bottom: 5px;">Time in Range Breakdown (Clinical 5-Tier Targets)</div>
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

            // Legend with unit-aware labels
            sb.append("""
            </div>
            <div class="tir-legend">
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-vlow);"></span> Very Low (${tirLabels.veryLowLabel}): ${"%.1f%%".format(tir.veryLowPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-low);"></span> Low (${tirLabels.lowLabel}): ${"%.1f%%".format(tir.targetLowPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-in-range);"></span> In Range (${tirLabels.inRangeLabel}): ${"%.1f%%".format(tir.inRangePercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-high);"></span> High (${tirLabels.highLabel}): ${"%.1f%%".format(tir.targetHighPercentage)}</div>
                <div class="legend-item"><span class="legend-dot" style="background: var(--status-vhigh);"></span> Very High (${tirLabels.veryHighLabel}): ${"%.1f%%".format(tir.veryHighPercentage)}</div>
            </div>
        </div>
""")

            // Trend Chart inside Executive Summary (guaranteed on Page 1)
            if (options.includeTrendChart && data.dailyTrendPoints.isNotEmpty()) {
                sb.append(generateSvgTrendChart(data, prefs))
            }

            sb.append("""
        <!-- Insulin & Carbs Summary Box -->
        <div class="insulin-summary-box" style="margin-top: 14px; border-top: 1px solid var(--border); padding-top: 12px;">
            <div style="font-size: 13px; font-weight: 600; margin-bottom: 8px;">Insulin & Nutrition Profile (Daily Averages)</div>
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
        } else if (options.includeTrendChart && data.dailyTrendPoints.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">Average Trend Line (${unitLabel})</div>
        ${generateSvgTrendChart(data, prefs)}
    </section>
""")
        }

        // Monthly Summary Section (if > 1 month and selected)
        if (options.includeMonthlySummary && data.isMultiMonth && data.monthlySummaries.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Monthly Data Summary</span>
            <span style="font-size: 12px; font-weight: normal; color: var(--text-secondary);">${data.monthlySummaries.size} Months Breakdown</span>
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

        // Weekly Summary Section (if selected)
        if (options.includeWeeklySummary && data.weeklySummaries.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Weekly Data Summary</span>
            <span style="font-size: 12px; font-weight: normal; color: var(--text-secondary);">${data.weeklySummaries.size} Weeks in Window</span>
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

        // Mealtime Summary Section (if selected)
        if (options.includeMealtimePatterns && data.mealPeriodAverages.isNotEmpty()) {
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

        // Detailed Logbook Section (if selected)
        if (options.includeLogbook && data.totalEntries.isNotEmpty()) {
            sb.append("""
    <section class="card">
        <div class="card-title">
            <span>Detailed Logbook</span>
            <span style="font-size: 12px; font-weight: normal; color: var(--text-secondary);">${data.totalEntries.size} Recorded Entries</span>
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
""")
        }

        // Footer at the end of the report
        sb.append("""
    <footer style="text-align: center; color: var(--text-secondary); font-size: 12px; margin-top: 24px; padding-bottom: 20px;">
        Generated by SugrNote
    </footer>

</div>
</body>
</html>
""")

        return sb.toString()
    }

    private fun generateSvgTrendChart(data: ExportReportData, prefs: UserPreferences): String {
        val points = data.dailyTrendPoints
        if (points.isEmpty()) return ""

        val unit = prefs.glucoseUnit
        val lowThreshDisplay = GlucoseUnitConverter.toDisplayValue(prefs.lowThresholdMgDl, unit)
        val highThreshDisplay = GlucoseUnitConverter.toDisplayValue(prefs.highThresholdMgDl, unit)

        val displayValues = points.map { GlucoseUnitConverter.toDisplayValue(it.averageMgDl, unit) }
        val minVal = min(displayValues.minOrNull() ?: lowThreshDisplay, lowThreshDisplay)
        val maxVal = max(displayValues.maxOrNull() ?: highThreshDisplay, highThreshDisplay)

        val yMin = max(0f, minVal * 0.85f)
        val yMax = maxVal * 1.15f
        val yRange = if (yMax == yMin) 1f else (yMax - yMin)

        val svgWidth = 840f
        val svgHeight = 200f
        val padLeft = 55f
        val padRight = 25f
        val padTop = 15f
        val padBottom = 30f

        val plotW = svgWidth - padLeft - padRight
        val plotH = svgHeight - padTop - padBottom

        fun getY(value: Float): Float {
            val ratio = (value - yMin) / yRange
            return padTop + plotH * (1f - ratio)
        }

        fun getX(index: Int): Float {
            return if (points.size <= 1) {
                padLeft + plotW / 2f
            } else {
                padLeft + (index.toFloat() / (points.size - 1)) * plotW
            }
        }

        val yHigh = getY(highThreshDisplay)
        val yLow = getY(lowThreshDisplay)

        // Build path coordinates
        val polylinePoints = StringBuilder()
        val circleMarkers = StringBuilder()

        points.forEachIndexed { i, p ->
            val v = GlucoseUnitConverter.toDisplayValue(p.averageMgDl, unit)
            val cx = getX(i)
            val cy = getY(v)
            polylinePoints.append(String.format(Locale.US, "%.1f,%.1f ", cx, cy))
            circleMarkers.append("""<circle cx="${"%.1f".format(Locale.US, cx)}" cy="${"%.1f".format(Locale.US, cy)}" r="3" fill="#1A73E8" stroke="#FFFFFF" stroke-width="1.5"><title>${p.date}: ${GlucoseUnitConverter.format(p.averageMgDl, unit)} ${unit.displayLabel}</title></circle>""")
        }

        val startDateLabel = points.first().date.format(DateTimeFormatter.ofPattern("MMM dd"))
        val endDateLabel = points.last().date.format(DateTimeFormatter.ofPattern("MMM dd"))
        val midIndex = points.size / 2
        val midDateLabel = if (points.size > 2) points[midIndex].date.format(DateTimeFormatter.ofPattern("MMM dd")) else ""
        val midX = getX(midIndex)

        val formatStr = if (unit == GlucoseUnit.MG_DL) "%.0f" else "%.1f"
        val yMaxLabel = String.format(Locale.US, formatStr, yMax)
        val yMinLabel = String.format(Locale.US, formatStr, yMin)
        val highLabel = String.format(Locale.US, formatStr, highThreshDisplay)
        val lowLabel = String.format(Locale.US, formatStr, lowThreshDisplay)

        return """
        <div class="trend-chart-box">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px;">
                <div style="font-size: 13px; font-weight: 600;">Average Trend Line (${unit.displayLabel})</div>
                <div style="font-size: 11px; color: var(--text-secondary);">Daily averages over period (${points.size} active days)</div>
            </div>
            <div class="chart-container">
                <svg viewBox="0 0 $svgWidth $svgHeight" width="100%" height="100%" style="overflow: visible;">
                    <!-- Shaded Target Range Band -->
                    <rect x="$padLeft" y="$yHigh" width="$plotW" height="${yLow - yHigh}" fill="#E8F5E9" fill-opacity="0.6"/>

                    <!-- Grid lines -->
                    <line x1="$padLeft" y1="$padTop" x2="${svgWidth - padRight}" y2="$padTop" stroke="#E0E0E0" stroke-width="1"/>
                    <line x1="$padLeft" y1="${padTop + plotH}" x2="${svgWidth - padRight}" y2="${padTop + plotH}" stroke="#E0E0E0" stroke-width="1"/>

                    <!-- High Threshold Line (Amber dashed) -->
                    <line x1="$padLeft" y1="$yHigh" x2="${svgWidth - padRight}" y2="$yHigh" stroke="#FF8F00" stroke-width="1.5" stroke-dasharray="5,5"/>
                    <text x="${padLeft - 8}" y="${yHigh + 4}" font-size="10" fill="#FF8F00" text-anchor="end" font-weight="600">$highLabel</text>

                    <!-- Low Threshold Line (Red dashed) -->
                    <line x1="$padLeft" y1="$yLow" x2="${svgWidth - padRight}" y2="$yLow" stroke="#E53935" stroke-width="1.5" stroke-dasharray="5,5"/>
                    <text x="${padLeft - 8}" y="${yLow + 4}" font-size="10" fill="#E53935" text-anchor="end" font-weight="600">$lowLabel</text>

                    <!-- Top/Bottom Y Labels -->
                    <text x="${padLeft - 8}" y="${padTop + 10}" font-size="10" fill="#757575" text-anchor="end">$yMaxLabel</text>
                    <text x="${padLeft - 8}" y="${padTop + plotH}" font-size="10" fill="#757575" text-anchor="end">$yMinLabel</text>

                    <!-- Polyline Trend -->
                    <polyline fill="none" stroke="#1A73E8" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" points="${polylinePoints.toString().trim()}"/>

                    <!-- Markers -->
                    $circleMarkers

                    <!-- X-Axis Labels -->
                    <text x="$padLeft" y="${svgHeight - 8}" font-size="10" fill="#757575" text-anchor="start">$startDateLabel</text>
                    ${if (midDateLabel.isNotEmpty()) """<text x="$midX" y="${svgHeight - 8}" font-size="10" fill="#757575" text-anchor="middle">$midDateLabel</text>""" else ""}
                    <text x="${svgWidth - padRight}" y="${svgHeight - 8}" font-size="10" fill="#757575" text-anchor="end">$endDateLabel</text>
                </svg>
            </div>
            <div style="display: flex; gap: 14px; justify-content: flex-end; font-size: 11px; color: var(--text-secondary); margin-top: 2px;">
                <span style="display: flex; align-items: center; gap: 4px;"><span style="width: 12px; height: 3px; background: #1A73E8; display: inline-block;"></span> Daily Average Trend</span>
                <span style="display: flex; align-items: center; gap: 4px;"><span style="width: 12px; height: 9px; background: #E8F5E9; border: 1px solid #C8E6C9; display: inline-block;"></span> Target Range (${data.tirLabels.inRangeLabel})</span>
            </div>
        </div>
        """
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
