package com.example.sugrnote.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogbookViewModel(
    private val repository: GlucoseRepository,
    settingsRepository: SettingsRepository,
    private val applicationContext: android.content.Context
) : ViewModel() {

    val entries: StateFlow<List<GlucoseEntry>> = repository.observeAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun deleteEntry(entry: GlucoseEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            // Notify widget to update
            com.example.sugrnote.widget.AddEntryWidgetProvider.sendUpdateBroadcast(applicationContext)
        }
    }
}
