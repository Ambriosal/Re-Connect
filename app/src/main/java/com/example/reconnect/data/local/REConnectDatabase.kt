package com.example.reconnect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, InteractionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class REConnectDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun interactionDao(): InteractionDao

    companion object {
        @Volatile
        private var INSTANCE: REConnectDatabase? = null

        fun getDatabase(context: Context): REConnectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    REConnectDatabase::class.java,
                    "reconnect_database"
                )
                    .fallbackToDestructiveMigration() // safe during dev — remove before real use
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}