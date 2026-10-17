package com.example.sugrnote.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A single scrollable wheel column built using VerticalPager for deterministic,
 * synchronous page selection with center alignment.
 */
@Composable
fun <T> WheelColumn(
    items: List<T>,
    state: PagerState,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 44.dp,
    formatItem: (T) -> String = { it.toString() }
) {
    val coroutineScope = rememberCoroutineScope()
    val halfVisible = visibleItemCount / 2

    VerticalPager(
        state = state,
        pageSize = PageSize.Fixed(itemHeight),
        contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
        modifier = modifier.height(itemHeight * visibleItemCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { page ->
        val isSelected = page == state.currentPage
        val distance = kotlin.math.abs(page - state.currentPage)

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
                        state.animateScrollToPage(page)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatItem(items[page]),
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

/**
 * Zero-app inspired scrollable drum/wheel picker bottom sheet.
 * Only allows past dates up to Today (no tomorrow/future dates).
 * Restricts time to current or past when Today is selected.
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
    val today = remember { LocalDate.now() }
    val now = remember { LocalTime.now() }

    // Dates range: past 60 days up to today (NO FUTURE DATES)
    val dates = remember {
        (-60..0).map { today.plusDays(it.toLong()) }
    }

    val safeInitialDate = remember(initialDate) {
        if (initialDate.isAfter(today)) today else initialDate
    }
    val initialDateIndex = remember(safeInitialDate) {
        val idx = dates.indexOf(safeInitialDate)
        if (idx >= 0) idx else dates.lastIndex
    }

    val safeInitialTime = remember(initialTime, safeInitialDate) {
        if (safeInitialDate >= today && initialTime.isAfter(now)) now else initialTime
    }

    // Minutes
    val minutes = remember { (0..59).toList() }
    val initialMinuteIndex = remember(safeInitialTime) { safeInitialTime.minute.coerceIn(0, 59) }

    // Hours
    val isPm = safeInitialTime.hour >= 12
    val hours24 = remember { (0..23).toList() }
    val hours12 = remember { (1..12).toList() }

    val initialHour24Index = remember(safeInitialTime) { safeInitialTime.hour.coerceIn(0, 23) }
    val initialHour12Index = remember(safeInitialTime) {
        val h12 = when (val h = safeInitialTime.hour % 12) {
            0 -> 12
            else -> h
        }
        hours12.indexOf(h12).coerceAtLeast(0)
    }

    // Pager states
    val datePagerState = rememberPagerState(initialPage = initialDateIndex) { dates.size }
    val minutePagerState = rememberPagerState(initialPage = initialMinuteIndex) { minutes.size }
    val hour24PagerState = rememberPagerState(initialPage = initialHour24Index) { hours24.size }
    val hour12PagerState = rememberPagerState(initialPage = initialHour12Index) { hours12.size }
    val amPmPagerState = rememberPagerState(initialPage = if (isPm) 1 else 0) { 2 }

    // If on Today and the selected hour is in the future, gently scroll back to current hour
    LaunchedEffect(datePagerState.currentPage, hour24PagerState.currentPage) {
        val isToday = datePagerState.currentPage == dates.lastIndex
        if (isToday) {
            val currentNow = LocalTime.now()
            if (is24Hour && hour24PagerState.currentPage > currentNow.hour) {
                hour24PagerState.animateScrollToPage(currentNow.hour)
            }
        }
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
                        state = datePagerState,
                        formatItem = { formatWheelDate(it, today) },
                        itemHeight = itemHeight,
                        visibleItemCount = visibleItemCount,
                        modifier = Modifier.weight(if (is24Hour) 1.8f else 1.5f)
                    )

                    if (is24Hour) {
                        // 24-hour hour column
                        WheelColumn(
                            items = hours24,
                            state = hour24PagerState,
                            formatItem = { "%02d".format(it) },
                            itemHeight = itemHeight,
                            visibleItemCount = visibleItemCount,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 12-hour hour column
                        WheelColumn(
                            items = hours12,
                            state = hour12PagerState,
                            formatItem = { "%d".format(it) },
                            itemHeight = itemHeight,
                            visibleItemCount = visibleItemCount,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Minute column
                    WheelColumn(
                        items = minutes,
                        state = minutePagerState,
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
                            state = amPmPagerState,
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
                    val selectedDate = dates[datePagerState.currentPage.coerceIn(dates.indices)]
                    val selectedMinute = minutes[minutePagerState.currentPage.coerceIn(minutes.indices)]
                    val selectedHour = if (is24Hour) {
                        hours24[hour24PagerState.currentPage.coerceIn(hours24.indices)]
                    } else {
                        val h12 = hours12[hour12PagerState.currentPage.coerceIn(hours12.indices)]
                        val isSelectedPm = amPmPagerState.currentPage == 1
                        when {
                            isSelectedPm && h12 < 12 -> h12 + 12
                            !isSelectedPm && h12 == 12 -> 0
                            else -> h12
                        }
                    }

                    val currentNow = LocalTime.now()
                    val candidateTime = LocalTime.of(selectedHour.coerceIn(0, 23), selectedMinute.coerceIn(0, 59))

                    // User can only log current time and time past
                    val finalTime = if (selectedDate == today && candidateTime.isAfter(currentNow)) {
                        currentNow
                    } else {
                        candidateTime
                    }

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
 * - "Mon 12 Oct"
 */
private fun formatWheelDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(date, today)
    return when (diff) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}
