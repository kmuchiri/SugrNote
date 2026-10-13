package com.example.sugrnote.ui.entry

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sugrnote.domain.model.ExerciseTiming
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.ui.overview.MyCustomFontFamily

/**
 * Compact outlined text field with reduced vertical height (42dp) and tight content padding
 * to eliminate wasted vertical space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 42.dp,
    placeholder: String? = null,
    trailingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height),
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        readOnly = readOnly,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                placeholder = placeholder?.let {
                    {
                        Text(
                            text = it,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                },
                trailingIcon = when {
                    trailingIcon != null -> trailingIcon
                    trailingText != null -> {
                        {
                            Text(
                                text = trailingText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                    else -> null
                },
                contentPadding = contentPadding,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) {
            Toast.makeText(context, "Entry saved", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    var showDateTimePicker by remember { mutableStateOf(false) }

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

    // Zero-style 3-column scrollable drum/wheel picker bottom sheet
    if (showDateTimePicker) {
        DateTimeWheelPickerSheet(
            initialDate = viewModel.date,
            initialTime = viewModel.time,
            is24Hour = prefs.is24HourClock,
            onConfirm = { date, time ->
                viewModel.onDateChanged(date)
                viewModel.onTimeChanged(time)
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Sugr Note",
                        fontFamily = MyCustomFontFamily,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.saveEntry() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Consolidated Date & Time entry (Zero app style)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDateTimePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "LOGGED AT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DateTimeUtils.formatDisplayDateTime(viewModel.date, viewModel.time, prefs),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    TextButton(
                        onClick = { showDateTimePicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            "Edit",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Glucose Reading Input (Slightly larger primary entry)
            Column {
                Text(
                    "Glucose (${prefs.glucoseUnit.displayLabel})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                CompactOutlinedTextField(
                    value = viewModel.glucoseValueText,
                    onValueChange = { viewModel.onGlucoseValueChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = viewModel.glucoseError != null,
                    singleLine = true
                )
                if (viewModel.glucoseError != null) {
                    Text(
                        text = viewModel.glucoseError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            // 3. Period & Meal Dropdown Selection (Default: Random, No Horizontal Scroll)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Period Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Period",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    var periodExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = periodExpanded,
                        onExpandedChange = { periodExpanded = it }
                    ) {
                        CompactOutlinedTextField(
                            value = viewModel.basePeriod.displayLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false }
                        ) {
                            listOf(
                                BasePeriod.RANDOM,
                                BasePeriod.FASTING,
                                BasePeriod.BEFORE_MEAL,
                                BasePeriod.AFTER_MEAL,
                                BasePeriod.BEFORE_SLEEP
                            ).forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.displayLabel) },
                                    onClick = {
                                        viewModel.onBasePeriodChanged(p)
                                        periodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Meal Dropdown (Visible only when Before Meal or After Meal is selected)
                if (viewModel.basePeriod == BasePeriod.BEFORE_MEAL || viewModel.basePeriod == BasePeriod.AFTER_MEAL) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Meal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        var mealExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = mealExpanded,
                            onExpandedChange = { mealExpanded = it }
                        ) {
                            CompactOutlinedTextField(
                                value = viewModel.mealType.displayLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = mealExpanded,
                                onDismissRequest = { mealExpanded = false }
                            ) {
                                MealType.entries.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.displayLabel) },
                                        onClick = {
                                            viewModel.onMealTypeChanged(m)
                                            mealExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Exercise Timing (3 Chips spanning width without scroll)
            Column {
                Text(
                    "Exercise",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseTiming.entries.forEach { timing ->
                        FilterChip(
                            selected = viewModel.exerciseTiming == timing,
                            onClick = { viewModel.onExerciseTimingChanged(timing) },
                            label = { Text(timing.displayLabel, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Conditional Exercise Details
                AnimatedVisibility(visible = viewModel.exerciseTiming == ExerciseTiming.BEFORE_EXERCISE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text("Intensity", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            var intensityExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = intensityExpanded,
                                onExpandedChange = { intensityExpanded = it }
                            ) {
                                CompactOutlinedTextField(
                                    value = viewModel.exerciseIntensity,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intensityExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = intensityExpanded,
                                    onDismissRequest = { intensityExpanded = false }
                                ) {
                                    listOf("Light", "Moderate", "Intense").forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                viewModel.onExerciseIntensityChanged(opt)
                                                intensityExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration (mins)", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            CompactOutlinedTextField(
                                value = viewModel.exerciseDurationText,
                                onValueChange = { viewModel.onExerciseDurationChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = viewModel.exerciseDurationError != null
                            )
                            if (viewModel.exerciseDurationError != null) {
                                Text(
                                    text = viewModel.exerciseDurationError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Insulin Type (4 Chips spanning width without scroll)
            Column {
                Text(
                    "Insulin",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsulinType.entries.forEach { type ->
                        FilterChip(
                            selected = viewModel.insulinType == type,
                            onClick = { viewModel.onInsulinTypeChanged(type) },
                            label = { Text(type.displayLabel, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Conditional Insulin Units (Side-by-side)
                AnimatedVisibility(visible = viewModel.insulinType != InsulinType.NONE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.insulinType == InsulinType.LONG_ACTING || viewModel.insulinType == InsulinType.BOTH) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Long Acting units", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(4.dp))
                                CompactOutlinedTextField(
                                    value = viewModel.longActingUnitsText,
                                    onValueChange = { viewModel.onLongActingUnitsChanged(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = viewModel.longActingError != null
                                )
                                if (viewModel.longActingError != null) {
                                    Text(
                                        text = viewModel.longActingError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        }

                        if (viewModel.insulinType == InsulinType.SHORT_ACTING || viewModel.insulinType == InsulinType.BOTH) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Short Acting units", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(4.dp))
                                CompactOutlinedTextField(
                                    value = viewModel.shortActingUnitsText,
                                    onValueChange = { viewModel.onShortActingUnitsChanged(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = viewModel.shortActingError != null
                                )
                                if (viewModel.shortActingError != null) {
                                    Text(
                                        text = viewModel.shortActingError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Food & Carb amount (Side-by-side Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Food", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewModel.hasFood,
                            onClick = { viewModel.onHasFoodChanged(true) },
                            label = { Text("Yes", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = !viewModel.hasFood,
                            onClick = { viewModel.onHasFoodChanged(false) },
                            label = { Text("No", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(
                        visible = viewModel.hasFood,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + slideInHorizontally(initialOffsetX = { -it / 2 }),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + slideOutHorizontally(targetOffsetX = { -it / 2 }),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Carb amount (g)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            CompactOutlinedTextField(
                                value = viewModel.carbAmountText,
                                onValueChange = { viewModel.onCarbAmountChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = viewModel.carbError != null,
                                singleLine = true
                            )
                            if (viewModel.carbError != null) {
                                Text(
                                    text = viewModel.carbError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Notes (Collapsible)
            var showNotes by remember { mutableStateOf(viewModel.notesText.isNotBlank()) }

            LaunchedEffect(viewModel.notesText) {
                if (viewModel.notesText.isNotBlank()) {
                    showNotes = true
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = { showNotes = !showNotes },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showNotes) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = if (showNotes) "Hide notes" else "Show notes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showNotes) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = viewModel.notesText,
                            onValueChange = { viewModel.onNotesChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            placeholder = { Text("Add notes...", style = MaterialTheme.typography.bodyMedium) },
                            supportingText = {
                                Text(
                                    text = "${viewModel.notesText.length} / 254",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
