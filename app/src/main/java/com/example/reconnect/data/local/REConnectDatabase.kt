package com.example.reconnect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class, InteractionEntity::class],
    version = 2,        // ← bump this from 1 to 2
    exportSchema = false
)
abstract class REConnectDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun interactionDao(): InteractionDao

    companion object {

        @Volatile
        private var INSTANCE: REConnectDatabase? = null

        // Describes exactly what SQL to run to bring a v1 database up to v2
        // Room runs this automatically when it detects an old schema on the device
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE interactions ADD COLUMN callCount INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun getDatabase(context: Context): REConnectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    REConnectDatabase::class.java,
                    "reconnect_database"
                )
                    .addMigrations(MIGRATION_1_2)   // ← replaces fallbackToDestructiveMigration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}