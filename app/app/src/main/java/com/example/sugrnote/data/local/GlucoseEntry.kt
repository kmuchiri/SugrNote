package com.example.sugrnote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sugrnote.domain.model.EntrySource
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period

@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val glucoseMgDl: Float,
    val dateTime: Long, // epoch millis
    val period: Period,
    val insulinType: InsulinType = InsulinType.NONE,
    val longActingUnits: Float? = null,
    val shortActingUnits: Float? = null,
    val hasFood: Boolean = false,
    val carbAmount: Float? = null,
    val exerciseIntensity: String? = null,
    val exerciseDuration: Int? = null,
    val sourceType: EntrySource = EntrySource.MANUAL,
    val imagePath: String? = null
)
