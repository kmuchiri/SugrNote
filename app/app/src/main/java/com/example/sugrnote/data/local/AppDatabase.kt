package com.example.sugrnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GlucoseEntry::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseEntryDao(): GlucoseEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE glucose_entries ADD COLUMN exerciseIntensity TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE glucose_entries ADD COLUMN exerciseDuration INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new exerciseTiming column
                db.execSQL("ALTER TABLE glucose_entries ADD COLUMN exerciseTiming TEXT NOT NULL DEFAULT 'NONE'")
                // Migrate existing BEFORE_EXERCISE / AFTER_EXERCISE period rows
                db.execSQL("UPDATE glucose_entries SET exerciseTiming = 'BEFORE_EXERCISE', period = 'RANDOM' WHERE period = 'BEFORE_EXERCISE'")
                db.execSQL("UPDATE glucose_entries SET exerciseTiming = 'AFTER_EXERCISE', period = 'RANDOM' WHERE period = 'AFTER_EXERCISE'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE glucose_entries ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glucose_tracker.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
