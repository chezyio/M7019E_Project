package com.m7019e.nobi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel // Correct import for viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.m7019e.nobi.ui.theme.NobiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun String.encode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.decode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

@Composable
fun BottomTabbedLayout(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val auth = FirebaseAuth.getInstance()

    val tabItems = listOf(
        Triple("Explore", Icons.Default.Home, "Explore"),
        Triple("Plan", Icons.Default.Home, "Plan"),
        Triple("Itineraries", Icons.Default.Favorite, "Itineraries"),
        Triple("Profile", Icons.Filled.AccountCircle, "Profile"),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(80.dp)
            ) {
                tabItems.forEachIndexed { index, (title, icon, _) ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(navController)
                1 -> PlanScreen(navController)
                2 -> {
                    if (auth.currentUser != null) {
                        ItinerariesScreen(navController)
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate("login")
                        }
                    }
                }
                3 -> ProfileActivity(navController = navController)
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: DestinationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room database and repository
        val database = AppDatabase.getDatabase(this)
        repository = DestinationRepository(database, cacheDir, this)

        // Setup network monitoring
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val lastFetchTimestamp = mutableStateOf(0L) // Track last successful fetch

        // Register network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("MainActivity", "Network available, attempting to fetch new data")
                repository.updateNetworkAvailability(true)
                repository.fetchOrLoadDestinations(lastFetchTimestamp)
            }

            override fun onLost(network: Network) {
                Log.d("MainActivity", "Network lost, switching to cached data")
                repository.updateNetworkAvailability(false)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        setContent {
            NobiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainNavigation(navController)
                }
            }
        }

        // Initial fetch or load
        repository.fetchOrLoadDestinations(lastFetchTimestamp)

        // Prepopulate database if empty
        repository.prepopulateDatabase()
    }

    override fun onDestroy() {
        super.onDestroy()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(object : ConnectivityManager.NetworkCallback() {})
        Log.d("MainActivity", "Network callback unregistered")
    }
}

// Data classes for reviews and activities
@Serializable
data class Review(
    val author: String,
    val content: String,
    val rating: Float
)

@Serializable
data class Activity(
    val name: String,
    val description: String
)

// Extension to format population
fun Long.toLocaleString(): String = String.format("%,d", this)

@Composable
fun MainNavigation(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser by remember { mutableStateOf(auth.currentUser) }

    NavHost(navController, startDestination = if (currentUser != null) "home" else "login") {
        composable("login") { LoginScreen(navController) }
        composable("home") { BottomTabbedLayout(navController) }
        composable("profile") { BottomTabbedLayout(navController) }
        composable("detail/{title}/{subtitle}/{imageUrl}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "No Title"
            val subtitle = backStackEntry.arguments?.getString("subtitle") ?: "No Subtitle"
            val imageUrl = backStackEntry.arguments?.getString("imageUrl")?.decode() ?: ""
            DetailScreen(title, subtitle, imageUrl, navController)
        }
        composable(
            "itineraryDetail/{destination}/{startDate}/{endDate}/{interests}/{itineraryText}"
        ) { backStackEntry ->
            val destination = backStackEntry.arguments?.getString("destination")?.decode() ?: ""
            val startDate = backStackEntry.arguments?.getString("startDate")?.decode() ?: ""
            val endDate = backStackEntry.arguments?.getString("endDate")?.decode() ?: ""
            val interests = backStackEntry.arguments?.getString("interests")?.decode()
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val itineraryText = backStackEntry.arguments?.getString("itineraryText")?.decode() ?: ""
            ItineraryDetailScreen(
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                interests = interests,
                itineraryText = itineraryText,
                navController = navController
            )
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold { padding ->
        val repository = (LocalContext.current as ComponentActivity).let {
            AppDatabase.getDatabase(it).let { db ->
                DestinationRepository(db, it.cacheDir, it)
            }
        }
        ExploreTabContent(
            navController = navController,
            repository = repository,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun ExploreTabContent(navController: NavController, repository: DestinationRepository, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val destinations by repository.destinations.collectAsState()
    val isOnline by repository.isNetworkAvailable.collectAsState()
    val isFetching by repository.isFetching.collectAsState()

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (destinations.isEmpty() && !isFetching) {
            item {
                Text(
                    text = if (isOnline) {
                        "No destinations available. Please try again later."
                    } else {
                        "No cached destinations available. Please connect to the internet to load countries."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isFetching) {
                        Text(
                            text = "Fetching new destinations...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else if (!isOnline) {
                        Text(
                            text = "Showing cached destinations (offline mode)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    destinations.forEach { destination ->
                        // Only show destinations with cached images in offline mode
                        if (isOnline || destination.cachePath?.let { File(it).exists() } == true) {
                            DestinationCard(
                                destination = destination,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    navController.navigate(
                                        "detail/${destination.title}/${destination.subtitle}/${destination.imageUrl.encode()}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

val mockReviews = listOf(
    Review(
        author = "Traveler123",
        content = "Amazing experience visiting the landmarks! Highly recommend.",
        rating = 4.5f
    ),
    Review(
        author = "AdventureSeeker",
        content = "The food and culture were fantastic, but some areas were crowded.",
        rating = 4.0f
    )
)

val mockActivities = listOf(
    Activity(
        name = "City Tour",
        description = "A guided tour through the city's historic landmarks."
    ),
    Activity(
        name = "Food Tasting",
        description = "Sample local cuisine at top restaurants."
    )
)

// Extension function for network check in Compose
@Composable
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Preview(showBackground = true)
@Composable
fun BottomTabbedLayoutPreview() {
    NobiTheme {
        val navController = rememberNavController()
        MainNavigation(navController)
    }
}