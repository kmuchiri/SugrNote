package com.example.sugrnote.domain.model

enum class InsulinType {
    NONE,
    LONG_ACTING,
    SHORT_ACTING,
    BOTH;

    val displayLabel: String
        get() = when (this) {
            NONE -> "None"
            LONG_ACTING -> "Long Acting"
            SHORT_ACTING -> "Short Acting"
            BOTH -> "Both"
        }
}
