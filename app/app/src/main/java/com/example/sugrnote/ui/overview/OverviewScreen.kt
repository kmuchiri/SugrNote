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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LatestReadingCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    latestEntry = latestEntry,
                    prefs = prefs,
                    onEntryClick = onEntryClick
                )

                InsulinIntake24hCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    insulinStats24h = insulinStats24h,
                    latestLongActingEntry = latestLongActingEntry,
                    latestShortActingEntry = latestShortActingEntry,
                    prefs = prefs
                )
            }

            TrendsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = { trendsViewModel.setTimeRange(it) },
                aggregatedData = aggregatedData,
                prefs = prefs
            )

            GlucoseAveragesCard(
                modifier = Modifier.fillMaxWidth(),
                periodStats = viewModel.periodStats,
                prefs = prefs
            )

            Spacer(Modifier.height(8.dp))

            AveragesByMealCard(
                modifier = Modifier.fillMaxWidth(),
                mealTimePeriodStats = viewModel.mealTimePeriodStats,
                prefs = prefs
            )

            Spacer(Modifier.height(8.dp))

            if (latestLongActingEntry != null || latestShortActingEntry != null || insulinStats24h.total > 0f) {
                AverageTotalInsulinCard(
                    modifier = Modifier.fillMaxWidth(),
                    insulinPeriodStats = viewModel.insulinPeriodStats
                )
            }
        }
    }
}
