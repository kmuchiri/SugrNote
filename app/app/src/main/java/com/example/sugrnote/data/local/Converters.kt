package com.example.sugrnote.data.local

import androidx.room.TypeConverter
import com.example.sugrnote.domain.model.EntrySource
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period

class Converters {
    @TypeConverter
    fun fromPeriod(value: Period): String = value.name

    @TypeConverter
    fun toPeriod(value: String): Period = Period.valueOf(value)

    @TypeConverter
    fun fromInsulinType(value: InsulinType): String = value.name

    @TypeConverter
    fun toInsulinType(value: String): InsulinType = InsulinType.valueOf(value)

    @TypeConverter
    fun fromEntrySource(value: EntrySource): String = value.name

    @TypeConverter
    fun toEntrySource(value: String): EntrySource = EntrySource.valueOf(value)
}
