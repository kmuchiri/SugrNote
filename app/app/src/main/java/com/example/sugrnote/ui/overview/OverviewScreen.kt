package com.example.sugrnote.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusInRange
import com.example.sugrnote.ui.theme.StatusLow
import java.time.Instant
import java.time.ZoneId
import com.example.sugrnote.domain.util.DateTimeUtils

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.sugrnote.R // Make sure to import your app's R class

val MyCustomFontFamily = FontFamily(
    Font(R.font.baflion)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    trendsViewModel: com.example.sugrnote.ui.trends.TrendsViewModel,
    onAddEntry: () -> Unit,
    onEntryClick: (Long) -> Unit = {}
) {
    val latestEntry by viewModel.latestEntry.collectAsState()
    val latestLongActingEntry by viewModel.latestLongActingEntry.collectAsState()
    val latestShortActingEntry by viewModel.latestShortActingEntry.collectAsState()
    val insulinStats24h by viewModel.insulinStats24h.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()
    val selectedTimeRange by trendsViewModel.selectedTimeRange.collectAsState()
    val aggregatedData by trendsViewModel.aggregatedData.collectAsState()
    val context = LocalContext.current

    val periods = StatsPeriod.entries
    var selectedGlucosePeriod by remember { mutableStateOf(StatsPeriod.DAYS_7) }

    val isDarkTheme = isSystemInDarkTheme()
    val shadowColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else DefaultShadowColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SugrNote",
                        fontFamily = MyCustomFontFamily,
                        fontSize = 28.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add entry")
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val latestStatusColor = latestEntry?.let { entry ->
                when (GlucoseStatus.fromValue(entry.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl)) {
                    GlucoseStatus.LOW -> StatusLow
                    GlucoseStatus.IN_RANGE -> StatusInRange
                    GlucoseStatus.HIGH -> StatusHigh
                }
            }
            val latestCardBgColor = latestStatusColor ?: MaterialTheme.colorScheme.surface
            val latestCardContentColor = if (latestStatusColor != null) Color.White else MaterialTheme.colorScheme.onSurface
            val latestCardVariantColor = if (latestStatusColor != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            val latestCardPrimaryColor = if (latestStatusColor != null) Color.White else MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latest Reading Card — left side
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 8.dp,
                            shape = CardDefaults.shape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        ),
                    onClick = {
                        latestEntry?.let { onEntryClick(it.id) }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = latestCardBgColor,
                        contentColor = latestCardContentColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Latest Reading",
                            style = MaterialTheme.typography.labelLarge,
                            color = latestCardVariantColor
                        )
                        Spacer(Modifier.height(8.dp))

                        if (latestEntry != null) {
                            val entry = latestEntry!!

                            Text(
                                GlucoseUnitConverter.format(
                                    entry.glucoseMgDl,
                                    prefs.glucoseUnit
                                ),
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                prefs.glucoseUnit.displayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = latestCardVariantColor
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                entry.period.displayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = latestCardPrimaryColor
                            )

                            Spacer(Modifier.height(4.dp))

                            val instant = Instant.ofEpochMilli(entry.dateTime)
                            val zoned = instant.atZone(ZoneId.systemDefault())
                            Text(
                                DateTimeUtils.formatRelativeDateTime(zoned, prefs),
                                style = MaterialTheme.typography.bodySmall,
                                color = latestCardVariantColor
                            )
                        } else {
                            Text(
                                "No readings yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = latestCardVariantColor
                            )
                        }
                    }
                }

                // Insulin (24h) Card — right side
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 8.dp,
                            shape = CardDefaults.shape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Insulin (24h)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        if (insulinStats24h.totalInjections > 0) {
                            val formatInsulin = { value: Float -> if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value) }

                            // Total units
                            Text(
                                "${formatInsulin(insulinStats24h.total)}u",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${insulinStats24h.totalInjections} injection(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            // Last Long Acting injection
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF9C27B0)))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Long Acting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (latestLongActingEntry != null) {
                                val longZoned = Instant.ofEpochMilli(latestLongActingEntry!!.dateTime)
                                    .atZone(ZoneId.systemDefault())
                                Text(
                                    DateTimeUtils.formatRelativeDateTime(longZoned, prefs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            } else {
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Last Short Acting injection
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2196F3)))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Short Acting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (latestShortActingEntry != null) {
                                val shortZoned = Instant.ofEpochMilli(latestShortActingEntry!!.dateTime)
                                    .atZone(ZoneId.systemDefault())
                                Text(
                                    DateTimeUtils.formatRelativeDateTime(shortZoned, prefs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            } else {
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }
                        } else {
                            Text(
                                "No injections",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Trends Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = CardDefaults.shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Trending Glucose",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    // Time Range Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        com.example.sugrnote.ui.trends.TimeRange.entries.forEach { range ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedTimeRange == range,
                                onClick = { trendsViewModel.setTimeRange(range) },
                                label = { Text(range.display) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
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
                            .height(250.dp)
                            .padding(top = 16.dp)
                    ) {
                        com.example.sugrnote.ui.trends.TrendsGraph(
                            data = aggregatedData,
                            prefs = prefs,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Glucose Averages
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = CardDefaults.shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Glucose Averages",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Time Period Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        periods.forEach { period ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedGlucosePeriod == period,
                                onClick = { selectedGlucosePeriod = period },
                                label = { Text(period.label) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val stats by viewModel.periodStats[selectedGlucosePeriod]!!.collectAsState()

                    if (stats.average != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    GlucoseUnitConverter.format(
                                        stats.average!!,
                                        prefs.glucoseUnit
                                    ),
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
                                    stats.count.toString(),
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
                        
                        val total = stats.count.toFloat()
                        val lowWeight = if (total > 0) stats.lowCount / total else 0f
                        val inRangeWeight = if (total > 0) stats.inRangeCount / total else 0f
                        val highWeight = if (total > 0) stats.highCount / total else 0f

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
                            // Status breakdown
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stats.lowCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusLow)
                                    Text("Low", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stats.inRangeCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusInRange)
                                    Text("Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stats.highCount.toString(), style = MaterialTheme.typography.titleMedium, color = StatusHigh)
                                    Text("High", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Min/Max breakdown
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        stats.min?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "-",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text("Lowest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        stats.max?.let { GlucoseUnitConverter.format(it, prefs.glucoseUnit) } ?: "-",
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

            Spacer(Modifier.height(8.dp))

            var selectedMealTimePeriod by remember { mutableStateOf(StatsPeriod.DAYS_7) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = CardDefaults.shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Averages By Meal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatsPeriod.entries.forEach { period ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedMealTimePeriod == period,
                                onClick = { selectedMealTimePeriod = period },
                                label = { Text(period.label) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    val mealTimeStats by viewModel.mealTimePeriodStats[selectedMealTimePeriod]!!.collectAsState()
                    val formatValue = { value: Float? -> 
                        if (value == null) "-" else GlucoseUnitConverter.format(value, prefs.glucoseUnit)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                formatValue(mealTimeStats.fastingAverage),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Fasting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                formatValue(mealTimeStats.randomAverage),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Random", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                formatValue(mealTimeStats.beforeMealAverage),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Before Meal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                formatValue(mealTimeStats.afterMealAverage),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("After Meal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                formatValue(mealTimeStats.beforeSleepAverage),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Before Sleep", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (latestLongActingEntry != null || latestShortActingEntry != null || insulinStats24h.total > 0f) {
                var selectedInsulinPeriod by remember { mutableStateOf(StatsPeriod.DAYS_7) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = CardDefaults.shape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            "Average Total Insulin",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatsPeriod.entries.forEach { period ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedInsulinPeriod == period,
                                    onClick = { selectedInsulinPeriod = period },
                                    label = { Text(period.label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        val insulinStats by viewModel.insulinPeriodStats[selectedInsulinPeriod]!!.collectAsState()
                        if (insulinStats.dailyAverage != null) {
                            val formatInsulin = { value: Float -> if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    "${formatInsulin(insulinStats.dailyAverage!!)}u",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        insulinStats.injectionsCount.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Injection(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            val totalFloat = insulinStats.dailyAverage!!
                            val longAvg = insulinStats.longActingAverage ?: 0f
                            val shortAvg = insulinStats.shortActingAverage ?: 0f

                            val longWeight = if (totalFloat > 0f) longAvg / totalFloat else 0f
                            val shortWeight = if (totalFloat > 0f) shortAvg / totalFloat else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                if (longWeight > 0f) {
                                    Box(modifier = Modifier.weight(longWeight).fillMaxHeight().background(Color(0xFF9C27B0)))
                                }
                                if (shortWeight > 0f) {
                                    Box(modifier = Modifier.weight(shortWeight).fillMaxHeight().background(Color(0xFF2196F3)))
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF9C27B0)))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${formatInsulin(longAvg)}u Long Acting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2196F3)))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${formatInsulin(shortAvg)}u Short Acting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }
    }
}
