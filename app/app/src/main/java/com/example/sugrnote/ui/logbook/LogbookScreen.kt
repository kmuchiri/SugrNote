package com.example.sugrnote.ui.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.model.ExerciseTiming
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusInRange
import com.example.sugrnote.ui.theme.StatusLow
import java.time.Instant
import java.time.ZoneId
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.ui.overview.MyCustomFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: LogbookViewModel,
    onEntryClick: (Long) -> Unit,
    onAddEntry: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    // Delete confirmation state
    var entryToDelete by remember { mutableStateOf<GlucoseEntry?>(null) }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this glucose reading? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { viewModel.deleteEntry(it) }
                    entryToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Logbook",
                        fontFamily = MyCustomFontFamily,
                        fontSize = 24.sp
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
        ) {
            var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val tabs = listOf("Glucose", "Dose")
            
            androidx.compose.material3.TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    androidx.compose.material3.Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (selectedTabIndex == 0) {
                // Glucose View
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No entries yet.\nTap + to add your first reading.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val groupedEntries = remember(entries) {
                        entries.groupBy {
                            val instant = Instant.ofEpochMilli(it.dateTime)
                            val zoned = instant.atZone(ZoneId.systemDefault())
                            DateTimeUtils.formatDayHeader(zoned, prefs)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        groupedEntries.forEach { (dateHeader, entriesForDate) ->
                            item(key = "header_$dateHeader") {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(entriesForDate, key = { it.id }) { entry ->
                                LogbookEntryCard(
                                    entry = entry,
                                    displayValue = GlucoseUnitConverter.format(
                                        entry.glucoseMgDl,
                                        prefs.glucoseUnit
                                    ),
                                    unitLabel = prefs.glucoseUnit.displayLabel,
                                    status = GlucoseStatus.fromValue(
                                        entry.glucoseMgDl,
                                        prefs.lowThresholdMgDl,
                                        prefs.highThresholdMgDl
                                    ),
                                    prefs = prefs,
                                    onClick = { onEntryClick(entry.id) },
                                    onDelete = { entryToDelete = entry }
                                )
                            }
                        }
                    }
                }
            } else {
                // Dose View
                val doseEntries = remember(entries) {
                    entries.filter { it.insulinType != InsulinType.NONE }
                }
                
                if (doseEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No dose entries yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val groupedDoses = remember(doseEntries) {
                        doseEntries.groupBy {
                            val instant = Instant.ofEpochMilli(it.dateTime)
                            val zoned = instant.atZone(ZoneId.systemDefault())
                            DateTimeUtils.formatDayHeader(zoned, prefs)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        groupedDoses.forEach { (dateHeader, dosesForDate) ->
                            item(key = "dose_header_$dateHeader") {
                                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val totalUnits = dosesForDate.sumOf { 
                                        (it.longActingUnits?.toDouble() ?: 0.0) + (it.shortActingUnits?.toDouble() ?: 0.0)
                                    }.toFloat()
                                    val formatInsulin = { value: Float -> if (value % 1 == 0f) value.toInt().toString() else value.toString() }
                                    Text(
                                        text = "Total Dose: ${formatInsulin(totalUnits)}u",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(dosesForDate, key = { "dose_${it.id}" }) { entry ->
                                LogbookDoseCard(
                                    entry = entry,
                                    prefs = prefs
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogbookDoseCard(
    entry: GlucoseEntry,
    prefs: com.example.sugrnote.data.settings.UserPreferences
) {
    val instant = Instant.ofEpochMilli(entry.dateTime)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val timeStr = DateTimeUtils.formatTime(zoned, prefs)
    val formatInsulin = { value: Float -> if (value % 1 == 0f) value.toInt().toString() else value.toString() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.insulinType.displayLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (entry.longActingUnits != null) {
                        Text(
                            "${formatInsulin(entry.longActingUnits!!)}u Long",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                        )
                    }
                    if (entry.shortActingUnits != null) {
                        Text(
                            "${formatInsulin(entry.shortActingUnits!!)}u Short",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                        )
                    }
                }

                if (entry.hasFood && entry.carbAmount != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${entry.carbAmount}g Carbs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun LogbookEntryCard(
    entry: GlucoseEntry,
    displayValue: String,
    unitLabel: String,
    status: GlucoseStatus,
    prefs: com.example.sugrnote.data.settings.UserPreferences,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val instant = Instant.ofEpochMilli(entry.dateTime)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val dateStr = DateTimeUtils.formatDate(zoned, prefs)
    val timeStr = DateTimeUtils.formatTime(zoned, prefs)

    val statusColor = when (status) {
        GlucoseStatus.LOW -> StatusLow
        GlucoseStatus.IN_RANGE -> StatusInRange
        GlucoseStatus.HIGH -> StatusHigh
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = statusColor
            ) {}

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$displayValue $unitLabel",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$dateStr • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val periodLabel = if (entry.exerciseTiming != ExerciseTiming.NONE) {
                        "${entry.period.displayLabel} · ${entry.exerciseTiming.displayLabel}"
                    } else {
                        entry.period.displayLabel
                    }
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.insulinType != InsulinType.NONE) {
                        Icon(
                            Icons.Default.Medication,
                            contentDescription = "Insulin taken",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (entry.hasFood) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = "Food logged",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
