package com.m7019e.nobi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

@Serializable
data class CountryResponse(
    val name: Name,
    val capital: List<String>?,
    val region: String,
    val population: Long,
    val flags: Flags
)

@Serializable
data class Name(
    val common: String
)

@Serializable
data class Flags(
    val png: String
)

class DestinationRepository(
    private val database: AppDatabase,
    private val cacheDir: File,
    private val context: Context
) {
    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> get() = _destinations

    private val _isNetworkAvailable = MutableStateFlow(context.isNetworkAvailable())
    val isNetworkAvailable: StateFlow<Boolean> get() = _isNetworkAvailable

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> get() = _isFetching

    init {
        // Collect Room Flow to update destinations reactively
        CoroutineScope(Dispatchers.IO).launch {
            database.destinationDao().getAllDestinationsFlow().collectLatest { cachedDestinations ->
                _destinations.value = cachedDestinations
                Log.d("DestinationRepository", "Room Flow emitted: ${cachedDestinations.size} destinations")
            }
        }
    }

    fun updateNetworkAvailability(isAvailable: Boolean) {
        _isNetworkAvailable.value = isAvailable
        Log.d("DestinationRepository", "Network availability updated: $isAvailable")
    }

    fun fetchOrLoadDestinations(lastFetchTimestamp: MutableState<Long>) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            if (_isNetworkAvailable.value) {
                // Only fetch if last fetch was over 24 hours ago or no data exists
                val currentTime = Instant.now().toEpochMilli()
                val existingDestinations = database.destinationDao().getAllDestinations()
                if (existingDestinations.isEmpty() || (currentTime - lastFetchTimestamp.value) > 24 * 60 * 60 * 1000) {
                    try {
                        _isFetching.value = true
                        val countries = fetchDestinationsFromApi()
                        // Save to Room database
                        database.destinationDao().insertDestinations(countries)
                        lastFetchTimestamp.value = currentTime
                        Log.d("DestinationRepository", "Fetched and saved ${countries.size} destinations")

                        // Schedule image caching
                        scheduleImageCaching(countries)
                    } catch (e: Exception) {
                        Log.e("DestinationRepository", "Error fetching countries: ${e.message}", e)
                    } finally {
                        _isFetching.value = false
                    }
                } else {
                    Log.d("DestinationRepository", "Using recent cached data, last fetch: ${lastFetchTimestamp.value}")
                }
            } else {
                Log.d("DestinationRepository", "Offline: Loading destinations from Room")
            }
        }
    }

    private fun fetchDestinationsFromApi(): List<Destination> {
        val url = URL("https://restcountries.com/v3.1/all?fields=name,capital,region,population,flags")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        return try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<List<CountryResponse>>(jsonString)
                    .take(20) // Limit to 20 countries
                    .map { country ->
                        Destination(
                            title = country.name.common,
                            subtitle = country.capital?.firstOrNull() ?: "Unknown Capital",
                            description = "Explore ${country.name.common}, a vibrant destination in ${country.region} with a population of ${country.population.toLocaleString()}.",
                            location = country.region,
                            imageUrl = country.flags.png,
                            cachePath = File(cacheDir, "${country.name.common.lowercase()}.png").absolutePath
                        )
                    }
            } else {
                Log.e("DestinationRepository", "Failed to fetch countries: HTTP $responseCode")
                emptyList()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun prepopulateDatabase() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val existingDestinations = database.destinationDao().getAllDestinations()
            if (existingDestinations.isEmpty()) {
                val defaultDestinations = listOf(
                    Destination(
                        title = "Sample Country",
                        subtitle = "Sample Capital",
                        description = "A sample destination for testing.",
                        location = "Sample Region",
                        imageUrl = "https://via.placeholder.com/150",
                        cachePath = File(cacheDir, "sample_country.png").absolutePath
                    )
                )
                database.destinationDao().insertDestinations(defaultDestinations)
                Log.d("DestinationRepository", "Prepopulated database with sample destinations")
            }
        }
    }

    private fun scheduleImageCaching(destinations: List<Destination>) {
        val workManager = WorkManager.getInstance(context)
        destinations.forEach { destination ->
            val cacheFile = File(cacheDir, "${destination.title.lowercase()}.png")
            val inputData = Data.Builder()
                .putString(ImageCacheWorker.KEY_IMAGE_URL, destination.imageUrl)
                .putString(ImageCacheWorker.KEY_OUTPUT_PATH, cacheFile.absolutePath)
                .build()

            val cacheRequest = OneTimeWorkRequestBuilder<ImageCacheWorker>()
                .setInputData(inputData)
                .build()

            workManager.enqueue(cacheRequest)
            Log.d("DestinationRepository", "Enqueued cache request for ${destination.title}")
        }
    }

    private fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}