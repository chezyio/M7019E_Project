package com.m7019e.nobi

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DestinationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("DestinationWorker", "Worker started at ${System.currentTimeMillis()}")
        try {
            val destinations = CountryApiService.fetchTenCountries()
            val destinationTitles = destinations.map { it.title }.joinToString(", ")
            Log.d("DestinationWorker", "Fetched new destinations: $destinationTitles")
            val database = AppDatabase.getInstance(applicationContext)
            val dao = database.destinationDao()
            database.withTransaction {
                dao.clearDestinations()
                dao.insertDestinations(destinations.map { it.toEntity() })
            }
            Log.d("DestinationWorker", "Successfully updated Room with new destinations")
            Result.success()
        } catch (e: Exception) {
            Log.e("DestinationWorker", "Worker failed: ${e.message}")
            Result.retry()
        }
    }
}