package com.example.sugrnote.domain.util

import com.example.sugrnote.domain.model.GlucoseUnit
import kotlin.math.roundToInt

object GlucoseUnitConverter {
    private const val CONVERSION_FACTOR = 18.0182f

    fun mgDlToMmolL(mgDl: Float): Float = mgDl / CONVERSION_FACTOR

    fun mmolLToMgDl(mmolL: Float): Float = mmolL * CONVERSION_FACTOR

    fun toDisplayValue(mgDl: Float, unit: GlucoseUnit): Float = when (unit) {
        GlucoseUnit.MG_DL -> mgDl
        GlucoseUnit.MMOL_L -> mgDlToMmolL(mgDl)
    }

    fun toMgDl(displayValue: Float, unit: GlucoseUnit): Float = when (unit) {
        GlucoseUnit.MG_DL -> displayValue
        GlucoseUnit.MMOL_L -> mmolLToMgDl(displayValue)
    }

    fun format(mgDl: Float, unit: GlucoseUnit): String = when (unit) {
        GlucoseUnit.MG_DL -> mgDl.roundToInt().toString()
        GlucoseUnit.MMOL_L -> String.format("%.1f", mgDlToMmolL(mgDl))
    }
}
