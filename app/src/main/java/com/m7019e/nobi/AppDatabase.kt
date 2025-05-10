package com.m7019e.nobi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DestinationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nobi_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}