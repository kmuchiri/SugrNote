package com.example.sugrnote.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.sugrnote.ui.overview.components.*
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
                LatestReadingCard(
                    modifier = Modifier.weight(1f),
                    latestEntry = latestEntry,
                    prefs = prefs,
                    shadowColor = shadowColor,
                    onEntryClick = onEntryClick
                )

                InsulinIntake24hCard(
                    modifier = Modifier.weight(1f),
                    insulinStats24h = insulinStats24h,
                    latestLongActingEntry = latestLongActingEntry,
                    latestShortActingEntry = latestShortActingEntry,
                    prefs = prefs,
                    shadowColor = shadowColor
                )
            }

            TrendsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = { trendsViewModel.setTimeRange(it) },
                aggregatedData = aggregatedData,
                prefs = prefs,
                shadowColor = shadowColor
            )

            GlucoseAveragesCard(
                modifier = Modifier.fillMaxWidth(),
                periodStats = viewModel.periodStats,
                prefs = prefs,
                shadowColor = shadowColor
            )

            Spacer(Modifier.height(8.dp))

            AveragesByMealCard(
                modifier = Modifier.fillMaxWidth(),
                mealTimePeriodStats = viewModel.mealTimePeriodStats,
                prefs = prefs,
                shadowColor = shadowColor
            )

            Spacer(Modifier.height(8.dp))

            if (latestLongActingEntry != null || latestShortActingEntry != null || insulinStats24h.total > 0f) {
                AverageTotalInsulinCard(
                    modifier = Modifier.fillMaxWidth(),
                    insulinPeriodStats = viewModel.insulinPeriodStats,
                    shadowColor = shadowColor
                )
            }
        }
    }
}
