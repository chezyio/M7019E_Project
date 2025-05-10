package com.m7019e.nobi

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

class DestinationsViewModel(application: Application) : AndroidViewModel(application) {
    private val _destinationsState = MutableStateFlow<DestinationsState>(DestinationsState.Loading)
    val destinationsState: StateFlow<DestinationsState> = _destinationsState

    private val sharedPreferences = application.getSharedPreferences("nobi_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        setupNetworkCallback()
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

    fun fetchDestinations() {
        viewModelScope.launch {
            _destinationsState.value = DestinationsState.Loading
            try {
                if (isNetworkAvailable()) {
                    val destinations = withContext(Dispatchers.IO) {
                        CountryApiService.fetchTenCountries()
                    }
                    // Cache the fetched destinations
                    withContext(Dispatchers.IO) {
                        sharedPreferences.edit()
                            .putString("cached_destinations", json.encodeToString(destinations))
                            .apply()
                    }
                    _destinationsState.value = DestinationsState.Success(destinations)
                } else {
                    // Load cached destinations if offline
                    val cachedJson = withContext(Dispatchers.IO) {
                        sharedPreferences.getString("cached_destinations", null)
                    }
                    if (cachedJson != null) {
                        val cachedDestinations = json.decodeFromString<List<Destination>>(cachedJson)
                        _destinationsState.value = DestinationsState.Success(cachedDestinations)
                    } else {
                        _destinationsState.value = DestinationsState.Error("No network and no cached data available")
                    }
                }
            } catch (e: Exception) {
                // Try loading cached data on failure
                val cachedJson = withContext(Dispatchers.IO) {
                    sharedPreferences.getString("cached_destinations", null)
                }
                if (cachedJson != null) {
                    val cachedDestinations = json.decodeFromString<List<Destination>>(cachedJson)
                    _destinationsState.value = DestinationsState.Success(cachedDestinations)
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
        super.onCleared()
        // Note: Network callback is not unregistered to allow background refetching.
        // If you want to unregister, you need to store the callback and request.
    }
}