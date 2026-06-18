package com.example.sugrnote.data.settings

import com.example.sugrnote.domain.model.GlucoseUnit

data class UserPreferences(
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val lowThresholdMgDl: Float = 70f,
    val highThresholdMgDl: Float = 180f
)
