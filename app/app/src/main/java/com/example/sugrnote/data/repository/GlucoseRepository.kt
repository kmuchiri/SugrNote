package com.example.sugrnote.data.repository

import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.local.GlucoseEntryDao
import kotlinx.coroutines.flow.Flow

class GlucoseRepository(private val dao: GlucoseEntryDao) {

    fun observeAllEntries(): Flow<List<GlucoseEntry>> = dao.observeAll()

    fun observeLatestEntry(): Flow<GlucoseEntry?> = dao.observeLatest()

    fun observeAverageSince(sinceMillis: Long): Flow<Float?> = dao.observeAverageSince(sinceMillis)

    fun observeCountSince(sinceMillis: Long): Flow<Int> = dao.observeCountSince(sinceMillis)

    fun observeEntriesSince(sinceMillis: Long): Flow<List<GlucoseEntry>> = dao.observeEntriesSince(sinceMillis)

    suspend fun getEntryById(id: Long): GlucoseEntry? = dao.getById(id)

    suspend fun insertEntry(entry: GlucoseEntry): Long = dao.insert(entry)

    suspend fun updateEntry(entry: GlucoseEntry) = dao.update(entry)

    suspend fun deleteEntry(entry: GlucoseEntry) = dao.delete(entry)

    fun observeLatestLongActing(): Flow<GlucoseEntry?> = dao.observeLatestLongActing()

    fun observeLatestShortActing(): Flow<GlucoseEntry?> = dao.observeLatestShortActing()
}
