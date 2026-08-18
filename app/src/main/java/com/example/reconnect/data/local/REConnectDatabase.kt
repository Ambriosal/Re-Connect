package com.example.reconnect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class, InteractionEntity::class],
    version = 5,        // ← bump this from 4 to 5
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE interactions ADD COLUMN needsFollowUp INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contacts ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contacts ADD COLUMN photoUri TEXT"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}