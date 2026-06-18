package com.example.sugrnote.domain.model

enum class GlucoseUnit {
    MG_DL,
    MMOL_L;

    val displayLabel: String
        get() = when (this) {
            MG_DL -> "mg/dL"
            MMOL_L -> "mmol/L"
        }
}
