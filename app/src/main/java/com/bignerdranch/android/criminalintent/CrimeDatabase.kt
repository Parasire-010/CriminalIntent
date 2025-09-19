package com.bignerdranch.android.criminalintent

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.bignerdranch.android.criminalintent.database.CrimeDao
import com.bignerdranch.android.criminalintent.database.CrimeTypeConverters

@Database(entities = [Crime::class], version = 6, exportSchema = false)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase : RoomDatabase() {
    abstract fun crimeDao(): CrimeDao

    companion object {
        @Volatile
        private var INSTANCE: CrimeDatabase? = null

        fun getDatabase(context: Context): CrimeDatabase {
            Log.d("CrimeDatabase", "Checking database path...")
            val dbPath = context.getDatabasePath("crime-database")
            Log.d("CrimeDatabase", "Database Path: ${dbPath.absolutePath}")
            if (!dbPath.exists()) {
                Log.w("CrimeDatabase", "Database does not exist!")
            } else {
                Log.d("CrimeDatabase", "Database exists and is ready.")
            }

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrimeDatabase::class.java,
                    "crime-database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6) // Register migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_4_5 = Migration(4, 5) { database ->
            Log.d("Migration", "Migrating from version 4 to 5...")
            database.execSQL("ALTER TABLE Crime ADD COLUMN suspect TEXT NOT NULL DEFAULT ''")
        }

        private val MIGRATION_5_6 = Migration(5, 6) { database ->
            Log.d("Migration", "Migrating from version 5 to 6...")
            database.execSQL("ALTER TABLE Crime ADD COLUMN photoFileName TEXT")
        }
    }
}
