package com.example.sugrnote.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Holds average + count for a given time period.
 */
data class PeriodStats(
    val average: Float?,
    val count: Int,
    val lowCount: Int,
    val inRangeCount: Int,
    val highCount: Int,
    val min: Float?,
    val max: Float?
)

/**
 * Defines the time periods available in the averages carousel.
 */
enum class StatsPeriod(val label: String, val daysBack: Int) {
    DAYS_7("7 Day", 7),
    DAYS_14("14 Day", 14),
    DAYS_30("30 Day", 30),
    DAYS_90("90 Day", 90);
}

class OverviewViewModel(
    repository: GlucoseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val latestEntry: StateFlow<GlucoseEntry?> = repository.observeLatestEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestLongActingEntry: StateFlow<GlucoseEntry?> = repository.observeLatestLongActing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestShortActingEntry: StateFlow<GlucoseEntry?> = repository.observeLatestShortActing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    /** Stats for each carousel period, keyed by enum. */
    val periodStats: Map<StatsPeriod, StateFlow<PeriodStats>> =
        StatsPeriod.entries.associateWith { period ->
            val sinceMillis = System.currentTimeMillis() - period.daysBack * 24 * 60 * 60 * 1000L
            combine(
                repository.observeEntriesSince(sinceMillis),
                userPreferences
            ) { entries, prefs ->
                if (entries.isEmpty()) {
                    PeriodStats(null, 0, 0, 0, 0, null, null)
                } else {
                    val count = entries.size
                    val average = entries.map { it.glucoseMgDl }.average().toFloat()
                    val min = entries.minOf { it.glucoseMgDl }
                    val max = entries.maxOf { it.glucoseMgDl }
                    var lowCount = 0
                    var inRangeCount = 0
                    var highCount = 0
                    
                    entries.forEach { entry ->
                        val status = com.example.sugrnote.domain.model.GlucoseStatus.fromValue(
                            entry.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl
                        )
                        when (status) {
                            com.example.sugrnote.domain.model.GlucoseStatus.LOW -> lowCount++
                            com.example.sugrnote.domain.model.GlucoseStatus.IN_RANGE -> inRangeCount++
                            com.example.sugrnote.domain.model.GlucoseStatus.HIGH -> highCount++
                        }
                    }
                    
                    PeriodStats(average, count, lowCount, inRangeCount, highCount, min, max)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodStats(null, 0, 0, 0, 0, null, null))
        }
}
