package com.example.sugrnote.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseEntryDao {
    @Insert
    suspend fun insert(entry: GlucoseEntry): Long

    @Update
    suspend fun update(entry: GlucoseEntry)

    @Delete
    suspend fun delete(entry: GlucoseEntry)

    @Query("SELECT * FROM glucose_entries ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<GlucoseEntry>>

    @Query("SELECT * FROM glucose_entries WHERE id = :id")
    suspend fun getById(id: Long): GlucoseEntry?

    @Query("SELECT * FROM glucose_entries ORDER BY dateTime DESC LIMIT 1")
    fun observeLatest(): Flow<GlucoseEntry?>

    @Query("SELECT AVG(glucoseMgDl) FROM glucose_entries WHERE dateTime >= :sinceMillis")
    fun observeAverageSince(sinceMillis: Long): Flow<Float?>

    @Query("SELECT COUNT(*) FROM glucose_entries WHERE dateTime >= :sinceMillis")
    fun observeCountSince(sinceMillis: Long): Flow<Int>
}
