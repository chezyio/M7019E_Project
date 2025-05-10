//package com.m7019e.nobi
//
//import android.app.Application
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import androidx.room.Room
//import androidx.work.Constraints
//import androidx.work.ExistingPeriodicWorkPolicy
//import androidx.work.ExistingWorkPolicy
//import androidx.work.NetworkType
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.PeriodicWorkRequestBuilder
//import androidx.work.WorkManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.concurrent.TimeUnit
//
//class DestinationsViewModel(application: Application) : AndroidViewModel(application) {
//    private val _destinationsState = MutableStateFlow<DestinationsState>(DestinationsState.Loading)
//    val destinationsState: StateFlow<DestinationsState> = _destinationsState
//
//    private val database = Room.databaseBuilder(
//        application,
//        AppDatabase::class.java,
//        "nobi_database"
//    ).build()
//    private val dao = database.destinationDao()
//    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    private val workManager = WorkManager.getInstance(application)
//
//    init {
//        setupNetworkCallback()
////        schedulePeriodicFetch()
//        observeCachedDestinations()
//        scheduleOneTimeFetch()
////        fetchDestinations()
//    }
//
//    private fun setupNetworkCallback() {
//        val networkCallback = object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                viewModelScope.launch {
//                    fetchDestinations()
//                }
//            }
//        }
//        val networkRequest = NetworkRequest.Builder()
//            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//            .build()
//        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//    }
//
////    private fun schedulePeriodicFetch() {
////        val constraints = Constraints.Builder()
////            .setRequiredNetworkType(NetworkType.CONNECTED)
////            .build()
////
////        val workRequest = PeriodicWorkRequestBuilder<DestinationWorker>(15, TimeUnit.MINUTES)
////            .setConstraints(constraints)
////            .build()
////
////        workManager.enqueueUniquePeriodicWork(
////            "destination_fetch",
////            ExistingPeriodicWorkPolicy.KEEP,
////            workRequest
////        )
////    }
//
//    private fun scheduleOneTimeFetch() {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        val workRequest = OneTimeWorkRequestBuilder<DestinationWorker>()
//            .setConstraints(constraints)
//            .build()
//
//        workManager.enqueueUniqueWork(
//            "destination_one_time_fetch",
//            ExistingWorkPolicy.REPLACE,
//            workRequest
//        )
//    }
//
//    private fun observeCachedDestinations() {
//        viewModelScope.launch {
//            dao.getAllDestinationsFlow().collectLatest { entities ->
//                if (entities.isNotEmpty()) {
//                    val destinationTitles = entities.map { it.title }.joinToString(", ")
//                    Log.d("DestinationsViewModel", "Loaded cached destinations from Room: $destinationTitles")
//                    _destinationsState.value = DestinationsState.Success(entities.map { it.toDestination() })
//                } else {
//                    Log.d("DestinationsViewModel", "No cached destinations found in Room")
//                }
//            }
//        }
//    }
//
//    fun fetchDestinations() {
//        viewModelScope.launch {
//            _destinationsState.value = DestinationsState.Loading
//            Log.d("DestinationsViewModel", "Fetching destinations, network available: ${isNetworkAvailable()}")
//            try {
//                if (isNetworkAvailable()) {
//                    val destinations = withContext(Dispatchers.IO) {
//                        CountryApiService.fetchTenCountries()
//                    }
//                    val destinationTitles = destinations.map { it.title }.joinToString(", ")
//                    Log.d("DestinationsViewModel", "Fetched new destinations from API: $destinationTitles")
//                    withContext(Dispatchers.IO) {
//                        dao.clearDestinations()
//                        dao.insertDestinations(destinations.map { it.toEntity() })
//                    }
//                    // State is updated via Room Flow observation
//                } else {
//                    val cachedDestinations = withContext(Dispatchers.IO) {
//                        dao.getAllDestinations()
//                    }
//                    if (cachedDestinations.isNotEmpty()) {
//                        val destinationTitles = cachedDestinations.map { it.title }.joinToString(", ")
//                        Log.d("DestinationsViewModel", "Loaded cached destinations (offline): $destinationTitles")
//                        _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
//                    } else {
//                        Log.d("DestinationsViewModel", "No cached destinations available and offline")
//                        _destinationsState.value = DestinationsState.Error("No network and no cached data available")
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("DestinationsViewModel", "Error fetching destinations: ${e.message}")
//                val cachedDestinations = withContext(Dispatchers.IO) {
//                    dao.getAllDestinations()
//                }
//                if (cachedDestinations.isNotEmpty()) {
//                    val destinationTitles = cachedDestinations.map { it.title }.joinToString(", ")
//                    Log.d("DestinationsViewModel", "Loaded cached destinations after error: $destinationTitles")
//                    _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
//                } else {
//                    Log.d("DestinationsViewModel", "No cached destinations available after error")
//                    _destinationsState.value = DestinationsState.Error("Failed to load destinations: ${e.message}")
//                }
//            }
//        }
//    }
//
//
//    private fun isNetworkAvailable(): Boolean {
//        val network = connectivityManager.activeNetwork ?: return false
//        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
//        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//    }
//
//    override fun onCleared() {
//        database.close()
//        super.onCleared()
//    }
//}


package com.m7019e.nobi

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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

    private val database = AppDatabase.getInstance(application)
    private val dao = database.destinationDao()
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val workManager = WorkManager.getInstance(application)

    // Track network state to detect restoration
    private var wasNetworkUnavailable = !isNetworkAvailable()

    init {
        setupNetworkCallback()
        scheduleOneTimeFetch()
        observeCachedDestinations()
        // check cache and fetch if empty and online (first launch)
        viewModelScope.launch {
            val cachedDestinations = withContext(Dispatchers.IO) {
                dao.getAllDestinations()
            }
            if (cachedDestinations.isEmpty() && isNetworkAvailable()) {
                Log.d("DestinationsViewModel", "No cache found and online, fetching destinations")
                fetchDestinations()
            }
        }
    }

    private fun setupNetworkCallback() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (wasNetworkUnavailable) {
                    viewModelScope.launch {
                        Log.d("DestinationsViewModel", "Network restored, fetching destinations")
                        fetchDestinations()
                    }
                    wasNetworkUnavailable = false
                } else {
                    Log.d("DestinationsViewModel", "Network callback ignored (network already available)")
                }
            }

            override fun onLost(network: Network) {
                wasNetworkUnavailable = true
                Log.d("DestinationsViewModel", "Network lost, marked as unavailable")
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun scheduleOneTimeFetch() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DestinationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // 1-minute delay for demo
            .build()

        workManager.enqueueUniqueWork(
            "destination_one_time_fetch",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("DestinationsViewModel", "Scheduled one-time WorkManager task with 1-minute delay")
    }

    private fun observeCachedDestinations() {
        viewModelScope.launch {
            dao.getAllDestinationsFlow().collectLatest { entities ->
                Log.d("DestinationsViewModel", "Flow emitted: ${entities.size} destinations")
                if (entities.isNotEmpty()) {
                    val destinationTitles = entities.map { it.title }.joinToString(", ")
                    Log.d("DestinationsViewModel", "Loaded cached destinations from Room: $destinationTitles")
                    _destinationsState.value = DestinationsState.Success(entities.map { it.toDestination() })
                } else {
                    Log.d("DestinationsViewModel", "No cached destinations found in Room")
                    _destinationsState.value = DestinationsState.Error("No cached data available")
                }
            }
        }
    }

    fun fetchDestinations() {
        viewModelScope.launch {
            _destinationsState.value = DestinationsState.Loading
            Log.d("DestinationsViewModel", "Fetching destinations, network available: ${isNetworkAvailable()}")
            try {
                if (isNetworkAvailable()) {
                    val destinations = withContext(Dispatchers.IO) {
                        CountryApiService.fetchTenCountries()
                    }
                    val destinationTitles = destinations.map { it.title }.joinToString(", ")
                    Log.d("DestinationsViewModel", "Fetched new destinations from API: $destinationTitles")
                    withContext(Dispatchers.IO) {
                        database.withTransaction {
                            dao.clearDestinations()
                            dao.insertDestinations(destinations.map { it.toEntity() })
                        }
                    }
                    // State is updated via Room Flow observation
                } else {
                    val cachedDestinations = withContext(Dispatchers.IO) {
                        dao.getAllDestinations()
                    }
                    if (cachedDestinations.isNotEmpty()) {
                        val destinationTitles = cachedDestinations.map { it.title }.joinToString(", ")
                        Log.d("DestinationsViewModel", "Loaded cached destinations (offline): $destinationTitles")
                        _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
                    } else {
                        Log.d("DestinationsViewModel", "No cached destinations available and offline")
                        _destinationsState.value = DestinationsState.Error("No network and no cached data available")
                    }
                }
            } catch (e: Exception) {
                Log.e("DestinationsViewModel", "Error fetching destinations: ${e.message}")
                val cachedDestinations = withContext(Dispatchers.IO) {
                    dao.getAllDestinations()
                }
                if (cachedDestinations.isNotEmpty()) {
                    val destinationTitles = cachedDestinations.map { it.title }.joinToString(", ")
                    Log.d("DestinationsViewModel", "Loaded cached destinations after error: $destinationTitles")
                    _destinationsState.value = DestinationsState.Success(cachedDestinations.map { it.toDestination() })
                } else {
                    Log.d("DestinationsViewModel", "No cached destinations available after error")
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
        super.onCleared()
    }
}