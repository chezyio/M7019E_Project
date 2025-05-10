package com.m7019e.nobi

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DestinationsViewModel(application: Application) : AndroidViewModel(application) {
    private val _destinationsState = MutableStateFlow<DestinationsState>(DestinationsState.Loading)
    val destinationsState: StateFlow<DestinationsState> = _destinationsState

    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "nobi_database"
    ).build()
    private val dao = database.destinationDao()
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val workManager = WorkManager.getInstance(application)

    init {
        setupNetworkCallback()
        schedulePeriodicFetch()
        observeCachedDestinations()
        fetchDestinations()
    }

    private fun setupNetworkCallback() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModelScope.launch {
                    fetchDestinations()
                }
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun schedulePeriodicFetch() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DestinationWorker>(30, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "destination_fetch",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun observeCachedDestinations() {
        viewModelScope.launch {
            dao.getAllDestinationsFlow().collectLatest { entities ->
                if (entities.isNotEmpty()) {
                    _destinationsState.value = DestinationsState.Success(entities.map { it.toDestination() })
                }
            }
        }
    }

    fun fetchDestinations() {
        viewModelScope.launch {
            _destinationsState.value = DestinationsState.Loading
            try {
                if (isNetworkAvailable()) {
                    val destinations = withContext(Dispatchers.IO) {
                        CountryApiService.fetchTenCountries()
                    }
                    withContext(Dispatchers.IO) {
                        dao.clearDestinations()
                        dao.insertDestinations(destinations.map { it.toEntity() })
                    }
                    // State is updated via Room Flow observation
                } else {
                    val cachedDestinations = withContext(Dispatchers.IO) {
                        dao.getAllDestinations()
                    }
                    if (cachedDestinations.isNotEmpty()) {
                        _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
                    } else {
                        _destinationsState.value = DestinationsState.Error("No network and no cached data available")
                    }
                }
            } catch (e: Exception) {
                val cachedDestinations = withContext(Dispatchers.IO) {
                    dao.getAllDestinations()
                }
                if (cachedDestinations.isNotEmpty()) {
                    _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
                } else {
                    _destinationsState.value = DestinationsState.Error("Failed to load destinations: ${e.message}")
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}