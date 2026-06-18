package com.example.sugrnote.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            glucoseUnit = prefs[Keys.GLUCOSE_UNIT]?.let {
                try { GlucoseUnit.valueOf(it) } catch (_: Exception) { GlucoseUnit.MG_DL }
            } ?: GlucoseUnit.MG_DL,
            lowThresholdMgDl = prefs[Keys.LOW_THRESHOLD] ?: 70f,
            highThresholdMgDl = prefs[Keys.HIGH_THRESHOLD] ?: 180f
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
}
