package com.example.sugrnote.ui.trends
import androidx.compose.foundation.Canvas
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusLow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val aggregatedData by viewModel.aggregatedData.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trending Glucose") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Time Range Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.display) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Graph Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            ) {
                TrendsGraph(
                    data = aggregatedData,
                    prefs = prefs,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TrendsGraph(
    data: com.example.sugrnote.ui.trends.TrendsData,
    prefs: com.example.sugrnote.data.settings.UserPreferences,
    color: Color
) {
    val lowThreshold = com.example.sugrnote.domain.util.GlucoseUnitConverter.toDisplayValue(prefs.lowThresholdMgDl.toFloat(), prefs.glucoseUnit)
    val highThreshold = com.example.sugrnote.domain.util.GlucoseUnitConverter.toDisplayValue(prefs.highThresholdMgDl.toFloat(), prefs.glucoseUnit)

    val (dataMax, dataMin) = when (data) {
        is TrendsData.Aggregated -> {
            val max = data.buckets.filterNotNull().maxOrNull() ?: highThreshold
            val min = data.buckets.filterNotNull().minOrNull() ?: lowThreshold
            Pair(max, min)
        }
        is TrendsData.Raw -> {
            val max = data.entries.maxOfOrNull { com.example.sugrnote.domain.util.GlucoseUnitConverter.toDisplayValue(it.glucoseMgDl.toFloat(), prefs.glucoseUnit) } ?: highThreshold
            val min = data.entries.minOfOrNull { com.example.sugrnote.domain.util.GlucoseUnitConverter.toDisplayValue(it.glucoseMgDl.toFloat(), prefs.glucoseUnit) } ?: lowThreshold
            Pair(max, min)
        }
    }

    val yMax = maxOf(dataMax, highThreshold) * 1.2f
    val yMin = minOf(dataMin, lowThreshold) * 0.8f
    val yRange = if (yMax == yMin) 100f else (yMax - yMin)

    val xLabelPaint = android.graphics.Paint().apply {
        textSize = 35f
        this.color = android.graphics.Color.DKGRAY
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    val yLabelPaint = android.graphics.Paint().apply {
        textSize = 35f
        this.color = android.graphics.Color.DKGRAY
        textAlign = android.graphics.Paint.Align.LEFT
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val rightPadding = 140f
        val yPadding = 80f
        val xPadding = 10f

        val graphWidth = width - xPadding - rightPadding
        val graphHeight = height - yPadding

        // 1. Draw High and Low Threshold lines
        val yPosHigh = graphHeight - ((highThreshold - yMin) / yRange * graphHeight)
        val yPosLow = graphHeight - ((lowThreshold - yMin) / yRange * graphHeight)
        
        drawLine(
            color = StatusHigh,
            start = Offset(xPadding, yPosHigh),
            end = Offset(xPadding + graphWidth, yPosHigh),
            strokeWidth = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        drawLine(
            color = StatusLow,
            start = Offset(xPadding, yPosLow),
            end = Offset(xPadding + graphWidth, yPosLow),
            strokeWidth = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // 2. Draw Y-axis labels and horizontal grid lines
        val ySteps = 5
        for (i in 0..ySteps) {
            val yValue = yMin + (yRange * i / ySteps)
            val yPos = graphHeight - (i.toFloat() / ySteps * graphHeight)

            val formatStr = if (prefs.glucoseUnit == com.example.sugrnote.domain.model.GlucoseUnit.MG_DL) "%.0f" else "%.1f"
            
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), formatStr, yValue),
                width - rightPadding + 20f,
                yPos + 12f,
                yLabelPaint
            )

            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(xPadding, yPos),
                end = Offset(xPadding + graphWidth, yPos),
                strokeWidth = 2f
            )
        }

        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(xPadding + graphWidth, 0f),
            end = Offset(xPadding + graphWidth, graphHeight),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(xPadding, graphHeight),
            end = Offset(xPadding + graphWidth, graphHeight),
            strokeWidth = 2f
        )

        if (data is TrendsData.Aggregated) {
            val buckets = 25
            val bucketWidth = graphWidth / (buckets - 1)

            for (i in 0 until buckets) {
                val xPos = xPadding + (i * bucketWidth)
                val hour = i
                drawLine(
                    color = Color.Gray,
                    start = Offset(xPos, graphHeight),
                    end = Offset(xPos, graphHeight + 15f),
                    strokeWidth = 2f
                )
                if (hour % 4 == 0) {
                    if (prefs.is24HourClock) {
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.getDefault(), "%02dH", hour),
                            xPos, graphHeight + 50f, xLabelPaint
                        )
                    } else {
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        drawContext.canvas.nativeCanvas.drawText(displayHour.toString(), xPos, graphHeight + 40f, xLabelPaint)
                        drawContext.canvas.nativeCanvas.drawText(amPm, xPos, graphHeight + 75f, xLabelPaint)
                    }
                }
            }

            val displayData = data.buckets.toMutableList()
            if (displayData.isNotEmpty()) {
                displayData.add(displayData.first())
            }
            val path = Path()
            var isFirst = true
            for (i in 0 until buckets) {
                val value = displayData.getOrNull(i)
                if (value != null) {
                    val xPos = xPadding + (i * bucketWidth)
                    val yPos = graphHeight - ((value - yMin) / yRange * graphHeight)
                    if (isFirst) {
                        path.moveTo(xPos, yPos)
                        isFirst = false
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                    drawCircle(color = color, radius = 6f, center = Offset(xPos, yPos))
                }
            }
            if (!isFirst) {
                drawPath(path = path, color = color, style = Stroke(width = 5f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        } else if (data is TrendsData.Raw) {
            val zoneId = ZoneId.systemDefault()
            val nowMillis = System.currentTimeMillis()
            val upcomingHourZoned = Instant.ofEpochMilli(nowMillis).atZone(zoneId).truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
            val endMillis = upcomingHourZoned.toInstant().toEpochMilli()
            val startMillis = upcomingHourZoned.minusHours(24).toInstant().toEpochMilli()
            val timeSpan = endMillis - startMillis

            for (h in 0..24) {
                val hourMillis = startMillis + (h * 60 * 60 * 1000L)
                val xPos = xPadding + ((hourMillis - startMillis).toFloat() / timeSpan) * graphWidth
                
                drawLine(
                    color = Color.Gray,
                    start = Offset(xPos, graphHeight),
                    end = Offset(xPos, graphHeight + 15f),
                    strokeWidth = 2f
                )

                if (h % 4 == 0 || h == 24) {
                    if (h == 24) {
                        drawContext.canvas.nativeCanvas.drawText("Now", xPos, graphHeight + 50f, xLabelPaint)
                    } else {
                        val tickZoned = Instant.ofEpochMilli(hourMillis).atZone(zoneId)
                        val hour = tickZoned.hour
                        if (prefs.is24HourClock) {
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.getDefault(), "%02dH", hour),
                                xPos, graphHeight + 50f, xLabelPaint
                            )
                        } else {
                            val amPm = if (hour < 12) "AM" else "PM"
                            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                            drawContext.canvas.nativeCanvas.drawText(displayHour.toString(), xPos, graphHeight + 40f, xLabelPaint)
                            drawContext.canvas.nativeCanvas.drawText(amPm, xPos, graphHeight + 75f, xLabelPaint)
                        }
                    }
                }
            }

            val path = Path()
            var isFirst = true
            
            // Sort entries chronologically
            val sortedEntries = data.entries.sortedBy { it.dateTime }
            
            for (entry in sortedEntries) {
                if (entry.dateTime in startMillis..endMillis) {
                    val xPos = xPadding + ((entry.dateTime - startMillis).toFloat() / timeSpan) * graphWidth
                    val value = com.example.sugrnote.domain.util.GlucoseUnitConverter.toDisplayValue(entry.glucoseMgDl.toFloat(), prefs.glucoseUnit)
                    val yPos = graphHeight - ((value - yMin) / yRange * graphHeight)
                    
                    if (isFirst) {
                        path.moveTo(xPos, yPos)
                        isFirst = false
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                    drawCircle(color = color, radius = 6f, center = Offset(xPos, yPos))
                }
            }
            if (!isFirst) {
                drawPath(path = path, color = color, style = Stroke(width = 5f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}
