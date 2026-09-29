package com.example.sugrnote.ui.trends

import com.example.sugrnote.data.local.GlucoseEntry

data class BucketData(val value: Float, val count: Int)

sealed class TrendsData {
    data class Aggregated(val buckets: List<BucketData?>) : TrendsData()
    data class Raw(val entries: List<GlucoseEntry>) : TrendsData()
}
