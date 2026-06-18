package com.example.sugrnote.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class OverviewViewModel(
    repository: GlucoseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val latestEntry: StateFlow<GlucoseEntry?> = repository.observeLatestEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val averageLast7Days: StateFlow<Float?> = repository.observeAverageSince(
        System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val countLast7Days: StateFlow<Int> = repository.observeCountSince(
        System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())
}
