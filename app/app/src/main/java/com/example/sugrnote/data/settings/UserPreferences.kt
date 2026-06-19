package com.example.sugrnote.data.settings

import com.example.sugrnote.domain.model.GlucoseUnit

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class DarkThemeStyle {
    STANDARD, OLED
}

data class UserPreferences(
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val lowThresholdMgDl: Float = 70f,
    val highThresholdMgDl: Float = 180f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.STANDARD
)
