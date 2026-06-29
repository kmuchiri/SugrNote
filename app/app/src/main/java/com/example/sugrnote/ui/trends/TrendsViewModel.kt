package com.example.sugrnote.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

enum class TimeRange(val display: String, val days: Int) {
    DAY_1("24H", 1),
    DAYS_7("7D", 7),
    DAYS_14("14D", 14),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90)
}

class TrendsViewModel(
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.DAY_1)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val entriesFlow = _selectedTimeRange.flatMapLatest { range ->
        val sinceMillis = System.currentTimeMillis() - (range.days * 24L * 60L * 60L * 1000L)
        glucoseRepository.observeEntriesSince(sinceMillis)
    }

    val aggregatedData: StateFlow<TrendsData> = combine(
        entriesFlow,
        userPreferences,
        _selectedTimeRange
    ) { entries, prefs, timeRange ->
        if (timeRange == TimeRange.DAY_1) {
            return@combine TrendsData.Raw(entries)
        }
        val buckets = Array<MutableList<Float>>(24) { mutableListOf() }
        val zoneId = ZoneId.systemDefault()

        entries.forEach { entry ->
            val instant = Instant.ofEpochMilli(entry.dateTime)
            val zoned = instant.atZone(zoneId)
            val hour = zoned.hour
            val minute = zoned.minute
            
            // Round to nearest hour: if minute >= 30, bucket is (hour + 1) % 24
            val bucketIndex = if (minute >= 30) {
                (hour + 1) % 24
            } else {
                hour
            }
            
            // We use the raw MgDl value for calculation, convert to user unit later
            buckets[bucketIndex].add(entry.glucoseMgDl.toFloat())
        }

        TrendsData.Aggregated(
            buckets.map { bucketList ->
                if (bucketList.isEmpty()) {
                    null
                } else {
                    val averageMgDl = bucketList.average().toFloat()
                    // Convert the average to the user's preferred unit
                    val convertedStr = GlucoseUnitConverter.format(averageMgDl, prefs.glucoseUnit)
                    convertedStr.toFloatOrNull()
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrendsData.Aggregated(List(24) { null }))

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }
}
