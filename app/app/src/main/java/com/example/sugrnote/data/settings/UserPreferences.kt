package com.example.sugrnote.data.settings

import com.example.sugrnote.domain.model.GlucoseUnit

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class DarkThemeStyle {
    STANDARD, OLED
}

enum class DateFormatOption(val pattern: String, val display: String) {
    DD_MMM_YYYY("dd MMM, yyyy", "22 Jun, 2026"),
    MMM_DD_YYYY("MMM dd, yyyy", "Jun 22, 2026")
}

data class UserPreferences(
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val lowThresholdMgDl: Float = 70f,
    val highThresholdMgDl: Float = 180f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.STANDARD,
    val is24HourClock: Boolean = false,
    val dateFormat: DateFormatOption = DateFormatOption.MMM_DD_YYYY
)
