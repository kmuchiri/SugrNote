package com.example.sugrnote.ui.overview.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import com.example.sugrnote.ui.overview.PeriodStats
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusInRange
import com.example.sugrnote.ui.theme.StatusLow
import com.example.sugrnote.ui.trends.TimeRange
import com.example.sugrnote.ui.trends.TrendsData
import com.example.sugrnote.ui.trends.TrendsGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsCard(
    modifier: Modifier = Modifier,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    aggregatedData: TrendsData,
    periodStats: PeriodStats,
    prefs: UserPreferences
) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Glucose Trend & Averages",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            
            // Time Range Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                TimeRange.entries.forEachIndexed { index, range ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index, 
                            count = TimeRange.entries.size,
                            baseShape = RoundedCornerShape(8.dp)
                        ),
                        onClick = { onTimeRangeSelected(range) },
                        selected = selectedTimeRange == range,
                        icon = {}
                    ) {
                        Text(range.display)
                    }
                }
            }

            // Graph Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                TrendsGraph(
                    data = aggregatedData,
                    prefs = prefs,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Averages and stats
            if (periodStats.average != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            GlucoseUnitConverter.format(periodStats.average, prefs.glucoseUnit),
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            prefs.glucoseUnit.displayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            periodStats.count.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Reading(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val total = periodStats.count.toFloat()
                val lowWeight = if (total > 0) periodStats.lowCount / total else 0f
                val inRangeWeight = if (total > 0) periodStats.inRangeCount / total else 0f
                val highWeight = if (total > 0) periodStats.highCount / total else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    if (lowWeight > 0f) {
                        Box(modifier = Modifier.weight(lowWeight).fillMaxHeight().background(StatusLow))
                    }
                    if (inRangeWeight > 0f) {
                        Box(modifier = Modifier.weight(inRangeWeight).fillMaxHeight().background(StatusInRange))
                    }
                    if (highWeight > 0f) {
                        Box(modifier = Modifier.weight(highWeight).fillMaxHeight().background(StatusHigh))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(periodStats.lowCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusLow)
                            Text("Low", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(periodStats.inRangeCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusInRange)
                            Text("Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(periodStats.highCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusHigh)
                            Text("High", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                periodStats.min?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "-",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Lowest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                periodStats.max?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "-",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Highest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Text(
                    "No data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
