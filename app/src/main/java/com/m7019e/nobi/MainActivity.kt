package com.m7019e.nobi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.ImeAction
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.ai.client.generativeai.GenerativeModel
import com.m7019e.nobi.ui.theme.NobiTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import coil.compose.rememberAsyncImagePainter
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.encode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.decode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

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
        composable("detail/{title}/{subtitle}/{imageUrl}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "No Title"
            val subtitle = backStackEntry.arguments?.getString("subtitle") ?: "No Subtitle"
            val imageUrl = backStackEntry.arguments?.getString("imageUrl")?.decode() ?: ""
            DetailScreen(title, subtitle, imageUrl, navController)
        }
        composable("overlay") { OverlayScreen(navController) }
        composable("aiTripPlanner") { AITripPlannerScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Login") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                auth.signInWithEmailAndPassword(email, password).await()
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Login failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Please enter email and password"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Logging in..." else "Login")
            }
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                auth.createUserWithEmailAndPassword(email, password).await()
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Registration failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Please enter email and password"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Registering..." else "Register")
            }
        }
    }
}

@Composable
fun BottomTabbedLayout(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val auth = FirebaseAuth.getInstance()

    val tabItems = listOf(
        Triple("Home", Icons.Default.Home, "Welcome to Home"),
        Triple("Favorites", Icons.Default.Favorite, "Your Favorite Items"),
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
                        FavoritesScreen(navController)
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate("login")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation (e.g., open drawer) */ }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("overlay") }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Open Overlay"
                        )
                    }
                }
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Text(
                    text = "Nobi",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { /* TODO: Add action */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Explore")
                    }
                    Button(
                        onClick = { navController.navigate("aiTripPlanner") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Plan")
                    }
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                mockDestinations.forEach { destination ->
                    SimpleCard(
                        destination = destination,
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { navController.navigate("detail/${destination.title}/${destination.subtitle}/${destination.imageUrl.encode()}") }
                    )
                }
            }
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
    Log.d("AITripPlannerScreen", "Raw itinerary: '$itinerary'")
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
    Log.d("AITripPlannerScreen", "Parsed intro: '$introText'")
    Log.d("AITripPlannerScreen", "Parsed plans: $dayPlans")
    return result
}

// check for bold syntax
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

data class Itinerary(
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val interests: List<String> = emptyList(),
    val itineraryText: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    var itineraries by remember { mutableStateOf<List<Itinerary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("itineraries")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            itineraries = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Itinerary::class.java)?.also {
                        Log.d("FavoritesScreen", "Deserialized itinerary: $it")
                    }
                } catch (e: Exception) {
                    Log.e("FavoritesScreen", "Failed to deserialize document ${doc.id}: ${e.message}", e)
                    null
                }
            }
            if (itineraries.isEmpty()) {
                Log.d("FavoritesScreen", "No itineraries found for user $userId")
            }
        } catch (e: Exception) {
            errorMessage = "Error loading itineraries: ${e.message}"
            Log.e("FavoritesScreen", "Error fetching itineraries: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favorites") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (itineraries.isEmpty()) {
                    item {
                        Text(
                            text = "No saved itineraries. Plan a trip with AI!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(itineraries) { itinerary ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Handle itinerary click if needed */ },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = itinerary.destination,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${itinerary.startDate} to ${itinerary.endDate}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Interests: ${itinerary.interests.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = itinerary.itineraryText.take(100) + if (itinerary.itineraryText.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITripPlannerScreen(navController: NavController) {
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
                    val prompt = "Create a $days-day itinerary for $destination from $startDate to $endDate, focusing on ${interests.joinToString(", ")}. Format each day as 'Day X:' followed by the activities."
                    Log.d("AITripPlannerScreen", "Generating with prompt: $prompt")
                    val response = generativeModel.generateContent(prompt)
                    itinerary = response.text ?: "No itinerary generated."
                    Log.d("AITripPlannerScreen", "Response: $itinerary")
                    showItineraryDialog = true
                } catch (e: Exception) {
                    itinerary = "Error generating itinerary: ${e.message}"
                    Log.e("AITripPlannerScreen", "Error: ${e.message}", e)
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

    fun saveItinerary() {
        val userId = auth.currentUser?.uid ?: return
        val itineraryData = Itinerary(
            destination = destination,
            startDate = startDate.format(dateFormatter),
            endDate = endDate.format(dateFormatter),
            interests = interests,
            itineraryText = itinerary,
            timestamp = System.currentTimeMillis()
        )

        coroutineScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("itineraries")
                    .add(itineraryData)
                    .await()
                saveStatus = "Itinerary saved successfully!"
            } catch (e: Exception) {
                saveStatus = "Error saving itinerary: ${e.message}"
                Log.e("AITripPlannerScreen", "Error saving itinerary: ${e.message}", e)
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("AI Trip Planner") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )

                TextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = { /* Read-only */ },
                    label = { Text("Start Date") },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
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

                TextField(
                    value = endDate.format(dateFormatter),
                    onValueChange = { /* Read-only */ },
                    label = { Text("End Date") },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
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

                Text(
                    text = "Interests:",
                    modifier = Modifier.padding(start = 16.dp)
                )
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Generating..." else "Generate Itinerary")
                }

                if (isLoading) {
                    Text(
                        text = "Loading itinerary...",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Text(
                        text = "Enter details and generate an itinerary to see your trip plan.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (saveStatus.isNotEmpty()) {
                    Text(
                        text = saveStatus,
                        color = if (saveStatus.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Itinerary Popover Dialog
                if (showItineraryDialog) {
                    AlertDialog(
                        onDismissRequest = { showItineraryDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = { saveItinerary() }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(title: String, subtitle: String, imageUrl: String, navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                ),
                contentDescription = "Detail Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Back to Home")
            }
        }
    }
}

@Composable
fun OverlayScreen(navController: NavController) {
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }), // Slide from right
            exit = slideOutHorizontally(targetOffsetX = { it })   // Slide to right
        ) {
            DrawerContent(
                onClose = {
                    scope.launch {
                        isVisible = false
                        // Delay to allow animation to complete before navigating back
                        kotlinx.coroutines.delay(300)
                        navController.popBackStack()
                    }
                },
                onLogout = {
                    scope.launch {
                        FirebaseAuth.getInstance().signOut()
                        isVisible = false
                        kotlinx.coroutines.delay(300)
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(onClose: () -> Unit, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val username = auth.currentUser?.email ?: "Guest"
    val db = Firebase.firestore
    var itineraries by remember { mutableStateOf<List<Itinerary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("itineraries")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            itineraries = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Itinerary::class.java)?.also {
                        Log.d("OverlayScreen", "Deserialized itinerary: $it")
                    }
                } catch (e: Exception) {
                    Log.e("OverlayScreen", "Failed to deserialize document ${doc.id}: ${e.message}", e)
                    null
                }
            }
            if (itineraries.isEmpty()) {
                Log.d("OverlayScreen", "No itineraries found for user $userId")
            }
        } catch (e: Exception) {
            errorMessage = "Error loading itineraries: ${e.message}"
            Log.e("OverlayScreen", "Error fetching itineraries: ${e.message}", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Overlay Menu",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Close"
                    )
                }
            }
        )

        // Username display
        Text(
            text = "Welcome, $username",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (itineraries.isEmpty()) {
                item {
                    Text(
                        text = "No saved itineraries",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(itineraries) { itinerary ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Handle itinerary click if needed */ },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = itinerary.destination,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${itinerary.startDate} to ${itinerary.endDate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Interests: ${itinerary.interests.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = itinerary.itineraryText.take(100) + if (itinerary.itineraryText.length > 100) "..." else "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Logout")
        }
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

data class TravelDestination(
    val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String
)

val mockDestinations = listOf(
    TravelDestination(
        title = "Paris",
        subtitle = "City of Light",
        description = "Explore the Eiffel Tower, Louvre Museum, and charming cafes along the Seine.",
        location = "France",
        imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&fm=jpg&w=1080&fit=max"
    ),
    TravelDestination(
        title = "Tokyo",
        subtitle = "Vibrant Metropolis",
        description = "Experience Shibuya Crossing, ancient temples, and world-class sushi.",
        location = "Japan",
        imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?q=80&fm=jpg&w=1080&fit=max"
    ),
    TravelDestination(
        title = "New York",
        subtitle = "The Big Apple",
        description = "Visit Times Square, Central Park, and the Statue of Liberty.",
        location = "USA",
        imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?q=80&fm=jpg&w=1080&fit=max"
    ),
    TravelDestination(
        title = "Rome",
        subtitle = "Eternal City",
        description = "Discover the Colosseum, Roman Forum, and authentic Italian cuisine.",
        location = "Italy",
        imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?q=80&fm=jpg&w=1080&fit=max"
    ),
    TravelDestination(
        title = "Cape Town",
        subtitle = "Coastal Gem",
        description = "Hike Table Mountain, visit Robben Island, and enjoy stunning beaches.",
        location = "South Africa",
        imageUrl = "https://images.unsplash.com/photo-1580062513330-c3cd672d9d74?q=80&fm=jpg&w=1080&fit=max"
    )
)