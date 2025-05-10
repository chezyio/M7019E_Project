package com.m7019e.nobi

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DestinationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
}