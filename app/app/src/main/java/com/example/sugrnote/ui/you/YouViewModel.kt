package com.example.sugrnote.ui.you

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.settings.DarkThemeStyle
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.ThemeMode
import com.example.sugrnote.data.settings.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class YouViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDarkThemeStyle(style: DarkThemeStyle) {
        viewModelScope.launch {
            settingsRepository.setDarkThemeStyle(style)
        }
    }
}
