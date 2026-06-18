package com.example.sugrnote.ui.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusInRange
import com.example.sugrnote.ui.theme.StatusLow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: LogbookViewModel,
    onEntryClick: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logbook") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No entries yet.\nTap + to add your first reading.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
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
                        onClick = { onEntryClick(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry) }
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val instant = Instant.ofEpochMilli(entry.dateTime)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val dateStr = zoned.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    val timeStr = zoned.format(DateTimeFormatter.ofPattern("hh:mm a"))

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
                    Text(
                        entry.period.displayLabel,
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
