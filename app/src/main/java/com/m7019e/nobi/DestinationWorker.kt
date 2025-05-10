package com.m7019e.nobi

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DestinationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val destinations = CountryApiService.fetchTenCountries()
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "nobi_database"
            ).build()
            val dao = database.destinationDao()
            dao.clearDestinations()
            dao.insertDestinations(destinations.map { it.toEntity() })
            database.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}