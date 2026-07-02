package com.example.sugrnote.domain.model

enum class Period {
    FASTING,
    BEFORE_BREAKFAST,
    AFTER_BREAKFAST,
    BEFORE_LUNCH,
    AFTER_LUNCH,
    BEFORE_SNACK,
    AFTER_SNACK,
    BEFORE_DINNER,
    AFTER_DINNER,
    BEFORE_SLEEP,
    BEFORE_EXERCISE,
    AFTER_EXERCISE,
    RANDOM;

    val displayLabel: String
        get() = when (this) {
            FASTING -> "Fasting"
            BEFORE_BREAKFAST -> "Before Breakfast"
            AFTER_BREAKFAST -> "After Breakfast"
            BEFORE_LUNCH -> "Before Lunch"
            AFTER_LUNCH -> "After Lunch"
            BEFORE_SNACK -> "Before Snack"
            AFTER_SNACK -> "After Snack"
            BEFORE_DINNER -> "Before Dinner"
            AFTER_DINNER -> "After Dinner"
            BEFORE_SLEEP -> "Before Sleep"
            BEFORE_EXERCISE -> "Before Exercise"
            AFTER_EXERCISE -> "After Exercise"
            RANDOM -> "Random"
        }
}
