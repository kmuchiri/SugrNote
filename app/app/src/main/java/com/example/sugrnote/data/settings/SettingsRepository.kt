package com.example.sugrnote.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sugrnote.domain.model.GlucoseUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val GLUCOSE_UNIT = stringPreferencesKey("glucose_unit")
        val LOW_THRESHOLD = floatPreferencesKey("low_threshold_mg_dl")
        val HIGH_THRESHOLD = floatPreferencesKey("high_threshold_mg_dl")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DARK_THEME_STYLE = stringPreferencesKey("dark_theme_style")
        val IS_24_HOUR_CLOCK = booleanPreferencesKey("is_24_hour_clock")
        val DATE_FORMAT = stringPreferencesKey("date_format")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            glucoseUnit = prefs[Keys.GLUCOSE_UNIT]?.let {
                try { GlucoseUnit.valueOf(it) } catch (_: Exception) { GlucoseUnit.MG_DL }
            } ?: GlucoseUnit.MG_DL,
            lowThresholdMgDl = prefs[Keys.LOW_THRESHOLD] ?: 70f,
            highThresholdMgDl = prefs[Keys.HIGH_THRESHOLD] ?: 180f,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                try { ThemeMode.valueOf(it) } catch (_: Exception) { ThemeMode.SYSTEM }
            } ?: ThemeMode.SYSTEM,
            darkThemeStyle = prefs[Keys.DARK_THEME_STYLE]?.let {
                try { DarkThemeStyle.valueOf(it) } catch (_: Exception) { DarkThemeStyle.STANDARD }
            } ?: DarkThemeStyle.STANDARD,
            is24HourClock = prefs[Keys.IS_24_HOUR_CLOCK] ?: false,
            dateFormat = prefs[Keys.DATE_FORMAT]?.let {
                try { DateFormatOption.valueOf(it) } catch (_: Exception) { DateFormatOption.MMM_DD_YYYY }
            } ?: DateFormatOption.MMM_DD_YYYY
        )
    }

    suspend fun setGlucoseUnit(unit: GlucoseUnit) {
        context.dataStore.edit { it[Keys.GLUCOSE_UNIT] = unit.name }
    }

    suspend fun setLowThreshold(mgDl: Float) {
        context.dataStore.edit { it[Keys.LOW_THRESHOLD] = mgDl }
    }

    suspend fun setHighThreshold(mgDl: Float) {
        context.dataStore.edit { it[Keys.HIGH_THRESHOLD] = mgDl }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDarkThemeStyle(style: DarkThemeStyle) {
        context.dataStore.edit { it[Keys.DARK_THEME_STYLE] = style.name }
    }

    suspend fun setIs24HourClock(is24Hour: Boolean) {
        context.dataStore.edit { it[Keys.IS_24_HOUR_CLOCK] = is24Hour }
    }

    suspend fun setDateFormat(format: DateFormatOption) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = format.name }
    }
}
