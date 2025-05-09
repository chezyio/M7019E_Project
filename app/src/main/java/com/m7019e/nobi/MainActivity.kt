package com.m7019e.nobi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
                1 -> {
                    if (auth.currentUser != null) {
                        ItinerariesScreen(navController)
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate("login")
                        }
                    }
                }
                2 -> {
                    ProfileActivity(navController = navController)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load cached destinations or fetch from API
        fetchOrLoadCountries()

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
    }

    private fun fetchOrLoadCountries() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            if (isNetworkAvailable()) {
                // Fetch from API if online
                try {
                    val url = URL("https://restcountries.com/v3.1/all?fields=name,capital,region,population,flags")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        inputStream.close()

                        val json = Json { ignoreUnknownKeys = true }
                        val countries = json.decodeFromString<List<CountryResponse>>(jsonString)
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

                        // Update global destinations list
                        destinations = countries

                        // Save to cache
                        saveDestinationsToCache(countries)

                        // Schedule image caching
                        scheduleImageCaching()
                    } else {
                        Log.e("MainActivity", "Failed to fetch countries: HTTP $responseCode")
                        loadDestinationsFromCache()
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error fetching countries: ${e.message}", e)
                    loadDestinationsFromCache()
                }
            } else {
                // Load from cache if offline
                loadDestinationsFromCache()
            }
        }
    }

    private fun saveDestinationsToCache(destinations: List<Destination>) {
        try {
            val cacheFile = File(cacheDir, "destinations.json")
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(destinations)
            cacheFile.writeText(jsonString)
            Log.d("MainActivity", "Saved destinations to cache: ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving destinations to cache: ${e.message}", e)
        }
    }

    private fun loadDestinationsFromCache() {
        try {
            val cacheFile = File(cacheDir, "destinations.json")
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                val json = Json { ignoreUnknownKeys = true }
                destinations = json.decodeFromString(jsonString)
                Log.d("MainActivity", "Loaded ${destinations.size} destinations from cache")
            } else {
                Log.d("MainActivity", "No cached destinations found")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading destinations from cache: ${e.message}", e)
            destinations = emptyList()
        }
    }

    private fun scheduleImageCaching() {
        val workManager = WorkManager.getInstance(this)
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
            Log.d("MainActivity", "Enqueued cache request for ${destination.title}")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// Data class for parsing REST Countries API response
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val tabs = listOf("Explore", "Plan")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View pager
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50.dp))
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(50.dp)
                                )
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // For tab content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ExploreTabContent(navController)
                    1 -> PlanTabContent(navController)
                }
            }
        }
    }
}

@Composable
fun ExploreTabContent(navController: NavController) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val isOffline = !context.isNetworkAvailable()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (isOffline && destinations.isEmpty()) {
            item {
                Text(
                    text = "No cached destinations available. Please connect to the internet to load countries.",
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
                    if (isOffline) {
                        Text(
                            text = "Showing cached destinations (offline mode)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    destinations.forEach { destination ->
                        // Only show destinations with cached images in offline mode
                        if (!isOffline || destination.cachePath?.let { File(it).exists() } == true) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTabContent(navController: NavController, viewModel: ItinerariesViewModel = viewModel()) {
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(5)) }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var itinerary by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showItineraryDialog by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("") }
    val parsedItinerary by remember(itinerary) { mutableStateOf(parseItinerary(itinerary)) }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().plusMillis(5 * 24 * 60 * 60 * 1000).toEpochMilli()
    )
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val availableInterests = listOf("History", "Food", "Adventure", "Art", "Nature", "Shopping")
    val apiKey = BuildConfig.GEMINI_KEY
    val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash-latest", apiKey = apiKey)

    val coroutineScope = rememberCoroutineScope()
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    fun generateItinerary() {
        if (destination.isNotBlank() && interests.isNotEmpty() && endDate >= startDate) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val days = endDate.toEpochDay() - startDate.toEpochDay() + 1
                    val prompt = "Create a $days-day itinerary for $destination from $startDate to $endDate, focusing on ${interests.joinToString(", ")}. Structure each day as ‘Day X: [Theme/Highlights]’ and divide the activities into sections: ‘Morning,’ ‘Afternoon,’ and ‘Evening.’ Group each activity/location with a title, description, and nothing else."
                    Log.d("PlanTabContent", "Generating with prompt: $prompt")
                    val response = generativeModel.generateContent(prompt)
                    itinerary = response.text ?: "No itinerary generated."
                    Log.d("PlanTabContent", "Response: $itinerary")
                    showItineraryDialog = true
                } catch (e: Exception) {
                    itinerary = "Error generating itinerary: ${e.message}"
                    Log.e("PlanTabContent", "Error: ${e.message}", e)
                    showItineraryDialog = true
                } finally {
                    isLoading = false
                }
            }
        } else {
            itinerary = "Please enter a destination, select interests, and ensure end date is not before start date."
            showItineraryDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            Text(
                text = "Loading itinerary...",
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "Enter a destination and select your interests",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = startDate.format(dateFormatter),
                onValueChange = { /* Read-only */ },
                label = { Text("Start Date") },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartDatePicker = true },
                enabled = false
            )
            if (showStartDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                startDatePickerState.selectedDateMillis?.let { millis ->
                                    startDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                                showStartDatePicker = false
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = startDatePickerState)
                }
            }

            OutlinedTextField(
                value = endDate.format(dateFormatter),
                onValueChange = { /* Read-only */ },
                label = { Text("End Date") },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndDatePicker = true },
                enabled = false
            )
            if (showEndDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                endDatePickerState.selectedDateMillis?.let { millis ->
                                    endDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                                showEndDatePicker = false
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = endDatePickerState)
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableInterests) { interest ->
                val selected = interests.contains(interest)
                FilterChip(
                    selected = selected,
                    onClick = {
                        interests = if (selected) {
                            interests - interest
                        } else {
                            interests + interest
                        }
                    },
                    label = { Text(interest) }
                )
            }
        }

        Button(
            onClick = { generateItinerary() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Generating..." else "Generate Itinerary")
        }

        if (viewModel.saveStatus.isNotEmpty()) {
            Text(
                text = viewModel.saveStatus,
                color = if (viewModel.saveStatus.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Itinerary Popover Dialog
        if (showItineraryDialog) {
            AlertDialog(
                onDismissRequest = { showItineraryDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveItinerary(
                                destination = destination,
                                startDate = startDate.format(dateFormatter),
                                endDate = endDate.format(dateFormatter),
                                interests = interests,
                                itineraryText = itinerary
                            )
                            showItineraryDialog = false
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showItineraryDialog = false }
                    ) { Text("Close") }
                },
                text = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (parsedItinerary.intro.isNotEmpty()) {
                                Text(
                                    text = parsedItinerary.intro,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (parsedItinerary.plans.isNotEmpty()) {
                                parsedItinerary.plans.forEach { plan ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = plan.day,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            plan.details.forEach { (key, value) ->
                                                Column {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = value,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Failed to parse itinerary: $itinerary",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

data class DayPlan(
    val day: String,
    val details: Map<String, String>
)

data class ParsedItinerary(
    val intro: String,
    val plans: List<DayPlan>
)

fun parseItinerary(itinerary: String): ParsedItinerary {
    Log.d("PlanTabContent", "Raw itinerary: '$itinerary'")
    val lines = itinerary.split("\n").filter { it.isNotBlank() }
    val introBuilder = StringBuilder()
    val dayPlans = mutableListOf<DayPlan>()
    var currentDay = ""
    val currentDetails = mutableListOf<String>()
    val boldPattern = Regex("\\*\\*(.+?)\\*\\*") // Matches **text**
    val dayPattern = Regex("^\\s*(?:\\*\\*)?Day\\s*\\d+[:\\s-].*?(?:\\*\\*)?$", RegexOption.IGNORE_CASE)

    lines.forEach { line ->
        val trimmedLine = line.trim()
        if (dayPattern.matches(trimmedLine)) {
            if (currentDay.isNotEmpty() && currentDetails.isNotEmpty()) {
                val detailsMap = parseDetails(currentDetails.joinToString("\n"), boldPattern)
                dayPlans.add(DayPlan(currentDay, detailsMap))
                currentDetails.clear()
            }
            currentDay = trimmedLine.replace("**", "")
        } else if (currentDay.isNotEmpty()) {
            currentDetails.add(trimmedLine)
        } else {
            introBuilder.append("$trimmedLine\n")
        }
    }
    if (currentDay.isNotEmpty() && currentDetails.isNotEmpty()) {
        val detailsMap = parseDetails(currentDetails.joinToString("\n"), boldPattern)
        dayPlans.add(DayPlan(currentDay, detailsMap))
    }

    val introText = introBuilder.toString().trim()
    if (dayPlans.isEmpty() && itinerary.isNotBlank()) {
        val detailsMap = parseDetails(itinerary.trim(), boldPattern)
        dayPlans.add(DayPlan("Day 1:", detailsMap))
    }

    val result = ParsedItinerary(introText, dayPlans)
    Log.d("PlanTabContent", "Parsed intro: '$introText'")
    Log.d("PlanTabContent", "Parsed plans: $dayPlans")
    return result
}

fun parseDetails(details: String, boldPattern: Regex): Map<String, String> {
    val sections = mutableMapOf<String, String>()
    val lines = details.split("\n").filter { it.isNotBlank() }
    var currentKey = "General"
    val currentContent = StringBuilder()

    lines.forEach { line ->
        val boldMatch = boldPattern.find(line)
        if (boldMatch != null) {
            if (currentContent.isNotEmpty()) {
                sections[currentKey] = currentContent.toString().trim()
                currentContent.clear()
            }
            currentKey = boldMatch.groupValues[1]
            val remainingText = line.substringAfter(boldMatch.value).trim()
            if (remainingText.isNotEmpty()) {
                currentContent.append("$remainingText\n")
            }
        } else {
            currentContent.append("$line\n")
        }
    }
    if (currentContent.isNotEmpty()) {
        sections[currentKey] = currentContent.toString().trim()
    }
    return sections
}

class ItinerariesViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private var listenerRegistration: ListenerRegistration? = null

    var itineraries by mutableStateOf<List<ItineraryWithId>>(emptyList())
        private set
    var errorMessage by mutableStateOf("")
        private set
    var saveStatus by mutableStateOf("")
        private set

    init {
        startListening()
    }

    private fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration = db.collection("users")
            .document(userId)
            .collection("itineraries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Error loading itineraries: ${error.message}"
                    Log.e("ItinerariesViewModel", "Listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    itineraries = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Itinerary::class.java)?.let {
                                ItineraryWithId(doc.id, it).also {
                                    Log.d("ItinerariesViewModel", "Deserialized itinerary: $it")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ItinerariesViewModel", "Failed to deserialize document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    errorMessage = ""
                    Log.d("ItinerariesViewModel", "Updated itineraries: ${itineraries.size} items")
                }
            }
    }

    fun saveItinerary(
        destination: String,
        startDate: String,
        endDate: String,
        interests: List<String>,
        itineraryText: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        val itineraryData = Itinerary(
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            interests = interests,
            itineraryText = itineraryText,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("itineraries")
                    .add(itineraryData)
                    .await()
                saveStatus = "Itinerary saved successfully!"
                Log.d("ItinerariesViewModel", "Saved itinerary: $itineraryData")
            } catch (e: Exception) {
                saveStatus = "Error saving itinerary: ${e.message}"
                Log.e("ItinerariesViewModel", "Error saving itinerary: ${e.message}", e)
            }
        }
    }

    fun deleteItinerary(itineraryId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("itineraries")
                    .document(itineraryId)
                    .delete()
                    .await()
                Log.d("ItinerariesViewModel", "Deleted itinerary $itineraryId")
            } catch (e: Exception) {
                errorMessage = "Error deleting itinerary: ${e.message}"
                Log.e("ItinerariesViewModel", "Error deleting itinerary: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        Log.d("ItinerariesViewModel", "Listener removed")
        super.onCleared()
    }
}

@Preview(showBackground = true)
@Composable
fun BottomTabbedLayoutPreview() {
    NobiTheme {
        val navController = rememberNavController()
        MainNavigation(navController)
    }
}

data class Itinerary(
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val interests: List<String> = emptyList(),
    val itineraryText: String = "",
    val timestamp: Long = 0L
)

data class ItineraryWithId(
    val id: String,
    val itinerary: Itinerary
)

data class Review(
    val author: String,
    val content: String,
    val rating: Float
)

data class Activity(
    val name: String,
    val description: String
)

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