package com.example.sugrnote.domain.model

enum class GlucoseStatus {
    LOW,
    IN_RANGE,
    HIGH;

    companion object {
        fun fromValue(glucoseMgDl: Float, lowThreshold: Float, highThreshold: Float): GlucoseStatus {
            return when {
                glucoseMgDl < lowThreshold -> LOW
                glucoseMgDl > highThreshold -> HIGH
                else -> IN_RANGE
            }
        }
    }
}
