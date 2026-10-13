package com.example.sugrnote.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A single scrollable wheel column with center-aligned snapping.
 */
@Composable
fun <T> WheelColumn(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (index: Int, item: T) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 44.dp,
    formatItem: (T) -> String = { it.toString() }
) {
    val coroutineScope = rememberCoroutineScope()
    val safeInitialIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(
        lazyListState = lazyListState,
        snapPosition = SnapPosition.Center
    )

    val halfVisible = visibleItemCount / 2

    // Determine the centered item index based on viewport center offset
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                safeInitialIndex
            } else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - centerOffset)
                }
                closest?.index?.coerceIn(0, items.size - 1) ?: safeInitialIndex
            }
        }
    }

    // Emit selection when scrolling ends
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { !it }
            .distinctUntilChanged()
            .collect {
                if (selectedIndex in items.indices) {
                    onItemSelected(selectedIndex, items[selectedIndex])
                }
            }
    }

    LazyColumn(
        state = lazyListState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
        modifier = modifier.height(itemHeight * visibleItemCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = index == selectedIndex
            val distance = kotlin.math.abs(index - selectedIndex)

            val textAlpha = when (distance) {
                0 -> 1f
                1 -> 0.55f
                2 -> 0.28f
                else -> 0.12f
            }

            val fontSize = when (distance) {
                0 -> 19.sp
                1 -> 16.sp
                else -> 14.sp
            }

            val fontStyle = if (distance > 0) FontStyle.Italic else FontStyle.Normal
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatItem(item),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Zero-app inspired 3-column (or 4-column) scrollable drum/wheel picker inside a ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeWheelPickerSheet(
    initialDate: LocalDate,
    initialTime: LocalTime,
    is24Hour: Boolean,
    onConfirm: (LocalDate, LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dates range: 60 days in the past up to 7 days ahead
    val dates = remember {
        (-60..7).map { LocalDate.now().plusDays(it.toLong()) }
    }
    val initialDateIndex = remember {
        dates.indexOf(initialDate).takeIf { it >= 0 } ?: dates.indexOf(LocalDate.now()).coerceAtLeast(0)
    }

    var selectedDate by remember { mutableStateOf(initialDate) }

    // Time representation
    val minutes = remember { (0..59).toList() }
    val initialMinuteIndex = remember { initialTime.minute.coerceIn(0, 59) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }

    // Hours
    val isPm = initialTime.hour >= 12
    var selectedIsPm by remember { mutableStateOf(isPm) }

    val hours24 = remember { (0..23).toList() }
    val hours12 = remember { (1..12).toList() }

    val initialHour24Index = remember { initialTime.hour.coerceIn(0, 23) }
    val initialHour12Index = remember {
        val h12 = when (val h = initialTime.hour % 12) {
            0 -> 12
            else -> h
        }
        hours12.indexOf(h12).coerceAtLeast(0)
    }

    var selectedHour24 by remember { mutableIntStateOf(initialTime.hour) }
    var selectedHour12 by remember {
        val h12 = when (val h = initialTime.hour % 12) {
            0 -> 12
            else -> h
        }
        mutableIntStateOf(h12)
    }

    val itemHeight = 44.dp
    val visibleItemCount = 5

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with Close 'X' icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Drum wheels container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * visibleItemCount)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Highlight band across the center row for all columns
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        )
                )

                // Wheel columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Column
                    WheelColumn(
                        items = dates,
                        initialIndex = initialDateIndex,
                        onItemSelected = { _, date -> selectedDate = date },
                        formatItem = { formatWheelDate(it) },
                        itemHeight = itemHeight,
                        visibleItemCount = visibleItemCount,
                        modifier = Modifier.weight(if (is24Hour) 1.8f else 1.5f)
                    )

                    if (is24Hour) {
                        // 24-hour hour column
                        WheelColumn(
                            items = hours24,
                            initialIndex = initialHour24Index,
                            onItemSelected = { _, h -> selectedHour24 = h },
                            formatItem = { "%02d".format(it) },
                            itemHeight = itemHeight,
                            visibleItemCount = visibleItemCount,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 12-hour hour column
                        WheelColumn(
                            items = hours12,
                            initialIndex = initialHour12Index,
                            onItemSelected = { _, h -> selectedHour12 = h },
                            formatItem = { "%d".format(it) },
                            itemHeight = itemHeight,
                            visibleItemCount = visibleItemCount,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Minute column
                    WheelColumn(
                        items = minutes,
                        initialIndex = initialMinuteIndex,
                        onItemSelected = { _, m -> selectedMinute = m },
                        formatItem = { "%02d".format(it) },
                        itemHeight = itemHeight,
                        visibleItemCount = visibleItemCount,
                        modifier = Modifier.weight(if (is24Hour) 1f else 0.9f)
                    )

                    if (!is24Hour) {
                        // AM / PM Column
                        val amPmList = remember { listOf("AM", "PM") }
                        WheelColumn(
                            items = amPmList,
                            initialIndex = if (isPm) 1 else 0,
                            onItemSelected = { _, str -> selectedIsPm = (str == "PM") },
                            formatItem = { it },
                            itemHeight = itemHeight,
                            visibleItemCount = visibleItemCount,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                // Top & Bottom gradient overlay for 3D drum fade effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight * 1.5f)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight * 1.5f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Update Button
            Button(
                onClick = {
                    val finalHour = if (is24Hour) {
                        selectedHour24
                    } else {
                        when {
                            selectedIsPm && selectedHour12 < 12 -> selectedHour12 + 12
                            !selectedIsPm && selectedHour12 == 12 -> 0
                            else -> selectedHour12
                        }
                    }
                    val finalTime = LocalTime.of(finalHour.coerceIn(0, 23), selectedMinute.coerceIn(0, 59))
                    onConfirm(selectedDate, finalTime)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "Update time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Format relative dates for the wheel picker:
 * - "Today"
 * - "Yesterday"
 * - "Tomorrow"
 * - "Mon 12 Oct"
 */
private fun formatWheelDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(date, today)
    return when (diff) {
        0L -> "Today"
        1L -> "Yesterday"
        -1L -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}
