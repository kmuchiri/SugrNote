package com.example.sugrnote.domain.model

enum class Period {
    FASTING,
    BEFORE_BREAKFAST,
    AFTER_BREAKFAST,
    BEFORE_LUNCH,
    AFTER_LUNCH,
    BEFORE_SLEEP,
    RANDOM;

    val displayLabel: String
        get() = when (this) {
            FASTING -> "Fasting"
            BEFORE_BREAKFAST -> "Before Breakfast"
            AFTER_BREAKFAST -> "After Breakfast"
            BEFORE_LUNCH -> "Before Lunch"
            AFTER_LUNCH -> "After Lunch"
            BEFORE_SLEEP -> "Before Sleep"
            RANDOM -> "Random"
        }
}
