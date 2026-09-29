package com.example.sugrnote.domain.model

enum class ExerciseTiming {
    NONE,
    BEFORE_EXERCISE,
    AFTER_EXERCISE;

    val displayLabel: String
        get() = when (this) {
            NONE -> "None"
            BEFORE_EXERCISE -> "Before Exercise"
            AFTER_EXERCISE -> "After Exercise"
        }
}
