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
    val count: Int
)

/**
 * Defines the time periods available in the averages carousel.
 */
enum class StatsPeriod(val label: String, val daysBack: Int) {
    DAYS_7("7 Days", 7),
    DAYS_14("14 Days", 14),
    MONTH_1("1 Month", 30),
    MONTHS_3("3 Months", 90);
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
                repository.observeAverageSince(sinceMillis),
                repository.observeCountSince(sinceMillis)
            ) { avg, cnt -> PeriodStats(avg, cnt) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodStats(null, 0))
        }
}
