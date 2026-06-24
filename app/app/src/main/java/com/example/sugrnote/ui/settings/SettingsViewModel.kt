package com.example.sugrnote.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseUnit
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val preferencesFlow = settingsRepository.preferencesFlow

    val userPreferences: StateFlow<UserPreferences> = preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    var lowThresholdText by mutableStateOf("")
        private set
    var highThresholdText by mutableStateOf("")
        private set
    var validationError by mutableStateOf<String?>(null)
        private set

    init {
        // Load real preferences from DataStore on init
        viewModelScope.launch {
            val prefs = preferencesFlow.first()
            updateThresholdDisplayTexts(prefs)
        }
    }

    private fun updateThresholdDisplayTexts(prefs: UserPreferences) {
        lowThresholdText = when (prefs.glucoseUnit) {
            GlucoseUnit.MG_DL -> prefs.lowThresholdMgDl.toInt().toString()
            GlucoseUnit.MMOL_L -> String.format(
                "%.1f",
                GlucoseUnitConverter.mgDlToMmolL(prefs.lowThresholdMgDl)
            )
        }
        highThresholdText = when (prefs.glucoseUnit) {
            GlucoseUnit.MG_DL -> prefs.highThresholdMgDl.toInt().toString()
            GlucoseUnit.MMOL_L -> String.format(
                "%.1f",
                GlucoseUnitConverter.mgDlToMmolL(prefs.highThresholdMgDl)
            )
        }
    }

    fun onLowThresholdChanged(text: String) {
        lowThresholdText = text
        validationError = null
    }

    fun onHighThresholdChanged(text: String) {
        highThresholdText = text
        validationError = null
    }

    fun onUnitChanged(unit: GlucoseUnit) {
        val currentPrefs = userPreferences.value
        viewModelScope.launch {
            settingsRepository.setGlucoseUnit(unit)
        }
        // Convert displayed thresholds to new unit
        lowThresholdText = when (unit) {
            GlucoseUnit.MG_DL -> currentPrefs.lowThresholdMgDl.toInt().toString()
            GlucoseUnit.MMOL_L -> String.format(
                "%.1f",
                GlucoseUnitConverter.mgDlToMmolL(currentPrefs.lowThresholdMgDl)
            )
        }
        highThresholdText = when (unit) {
            GlucoseUnit.MG_DL -> currentPrefs.highThresholdMgDl.toInt().toString()
            GlucoseUnit.MMOL_L -> String.format(
                "%.1f",
                GlucoseUnitConverter.mgDlToMmolL(currentPrefs.highThresholdMgDl)
            )
        }
    }

    fun saveThresholds() {
        val prefs = userPreferences.value
        val lowDisplay = lowThresholdText.toFloatOrNull()
        val highDisplay = highThresholdText.toFloatOrNull()

        if (lowDisplay == null || highDisplay == null) {
            validationError = "Enter valid numbers"
            return
        }

        val lowMgDl = GlucoseUnitConverter.toMgDl(lowDisplay, prefs.glucoseUnit)
        val highMgDl = GlucoseUnitConverter.toMgDl(highDisplay, prefs.glucoseUnit)

        if (lowMgDl >= highMgDl) {
            validationError = "Low threshold must be less than high threshold"
            return
        }

        viewModelScope.launch {
            settingsRepository.setLowThreshold(lowMgDl)
            settingsRepository.setHighThreshold(highMgDl)
        }
        validationError = null
    }

    fun onIs24HourClockChanged(is24Hour: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIs24HourClock(is24Hour)
        }
    }

    fun onDateFormatChanged(format: com.example.sugrnote.data.settings.DateFormatOption) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
        }
    }
}
