package com.m7019e.nobi

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.m7019e.nobi.ui.theme.NobiTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
}

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
            // view pager
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

            // for tab content
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
    val destinationsState = rememberDestinations()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (destinationsState) {
                    is DestinationsState.Loading -> {
                        Text(
                            text = "Loading destinations...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    is DestinationsState.Success -> {
                        destinationsState.destinations.forEach { destination ->
                            DestinationCard(
                                destination = destination,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    navController.navigate(
                                        "detail/${destination.title.encode()}/${destination.subtitle.encode()}/${destination.imageUrl.encode()}"
                                    )
                                }
                            )
                        }
                    }
                    is DestinationsState.Error -> {
                        Text(
                            text = destinationsState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
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
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between the fields
        ) {
            OutlinedTextField(
                value = startDate.format(dateFormatter),
                onValueChange = { /* Read-only */ },
                label = { Text("Start Date") },
                modifier = Modifier
                    .weight(1f) // Distribute space evenly
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
                    .weight(1f) // Distribute space evenly
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

        // itinerary Popover Dialog
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
    val boldPattern = Regex("\\*\\*(.+?)\\*\\*") // matches **text**
    val dayPattern = Regex("^\\s*(?:\\*\\*)?Day\\s*\\d+[:\\s-].*?(?:\\*\\*)?$", RegexOption.IGNORE_CASE)

    lines.forEach { line ->
        val trimmedLine = line.trim()
        if (dayPattern.matches(trimmedLine)) {
            if (currentDay.isNotEmpty() && currentDetails.isNotEmpty()) {
                val detailsMap = parseDetails(currentDetails.joinToString("\n"), boldPattern)
                dayPlans.add(DayPlan(currentDay, detailsMap))
                currentDetails.clear()
            }
            currentDay = trimmedLine.replace("**", "") // remove markdown bold
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

    // if no days parsed, treat remaining text as "Day 1"
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
    var currentKey = "General" // Default key for non-bolded text
    val currentContent = StringBuilder()

    lines.forEach { line ->
        val boldMatch = boldPattern.find(line)
        if (boldMatch != null) {
            if (currentContent.isNotEmpty()) {
                sections[currentKey] = currentContent.toString().trim()
                currentContent.clear()
            }
            currentKey = boldMatch.groupValues[1] // Extract text between **
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
                    errorMessage = "" // Clear error on successful update
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