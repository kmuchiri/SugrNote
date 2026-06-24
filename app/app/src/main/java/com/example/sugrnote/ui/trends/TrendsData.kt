package com.example.sugrnote.ui.trends

import com.example.sugrnote.data.local.GlucoseEntry

sealed class TrendsData {
    data class Aggregated(val buckets: List<Float?>) : TrendsData()
    data class Raw(val entries: List<GlucoseEntry>) : TrendsData()
}
