package com.example.sugrnote.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onNavigateBack()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (viewModel.showUnusualValueDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissUnusualValueDialog() },
            title = { Text("Unusual Value") },
            text = { Text("This glucose value seems unusual. Save anyway?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmUnusualValue() }) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissUnusualValueDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.onDateChanged(selected)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = viewModel.time.hour,
            initialMinute = viewModel.time.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChanged(
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Sugr Note") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date field
            Text("Date", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = viewModel.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, "Select date")
                    }
                }
            )

            // Time field
            Text("Time", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = viewModel.time.format(DateTimeFormatter.ofPattern("hh:mm a")),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.Schedule, "Select time")
                    }
                }
            )

            // Glucose value
            Text(
                "Glucose (${prefs.glucoseUnit.displayLabel})",
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedTextField(
                value = viewModel.glucoseValueText,
                onValueChange = { viewModel.onGlucoseValueChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = viewModel.glucoseError != null,
                supportingText = viewModel.glucoseError?.let { { Text(it) } },
                singleLine = true
            )

            // Period selection
            Text("Period", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = viewModel.period == p,
                        onClick = { viewModel.onPeriodChanged(p) },
                        label = { Text(p.displayLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Insulin type
            Text("Insulin", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsulinType.entries.forEach { type ->
                    FilterChip(
                        selected = viewModel.insulinType == type,
                        onClick = { viewModel.onInsulinTypeChanged(type) },
                        label = { Text(type.displayLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Long Acting units (conditional)
            AnimatedVisibility(
                visible = viewModel.insulinType == InsulinType.LONG_ACTING ||
                        viewModel.insulinType == InsulinType.BOTH
            ) {
                Column {
                    Text("Long Acting units", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = viewModel.longActingUnitsText,
                        onValueChange = { viewModel.onLongActingUnitsChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = viewModel.longActingError != null,
                        supportingText = viewModel.longActingError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            }

            // Short Acting units (conditional)
            AnimatedVisibility(
                visible = viewModel.insulinType == InsulinType.SHORT_ACTING ||
                        viewModel.insulinType == InsulinType.BOTH
            ) {
                Column {
                    Text("Short Acting units", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = viewModel.shortActingUnitsText,
                        onValueChange = { viewModel.onShortActingUnitsChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = viewModel.shortActingError != null,
                        supportingText = viewModel.shortActingError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            }

            // Food
            Text("Food", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.hasFood,
                    onClick = { viewModel.onHasFoodChanged(true) },
                    label = { Text("Yes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = !viewModel.hasFood,
                    onClick = { viewModel.onHasFoodChanged(false) },
                    label = { Text("No") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Carb amount (conditional)
            AnimatedVisibility(visible = viewModel.hasFood) {
                Column {
                    Text("Carb amount (g)", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = viewModel.carbAmountText,
                        onValueChange = { viewModel.onCarbAmountChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = viewModel.carbError != null,
                        supportingText = viewModel.carbError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { viewModel.saveEntry() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }
        }
    }
}
