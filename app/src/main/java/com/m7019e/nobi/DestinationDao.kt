package com.m7019e.nobi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestinations(destinations: List<DestinationEntity>)

    @Query("SELECT * FROM destinations")
    suspend fun getAllDestinations(): List<DestinationEntity>

    @Query("SELECT * FROM destinations")
    fun getAllDestinationsFlow(): Flow<List<DestinationEntity>>

    @Query("DELETE FROM destinations")
    suspend fun clearDestinations()
}