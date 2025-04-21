package com.m7019e.nobi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.m7019e.nobi.ui.theme.NobiTheme
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.encode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.decode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())


@Composable
fun BottomTabbedLayout(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val auth = FirebaseAuth.getInstance()

    val tabItems = listOf(
        Triple("Explore", Icons.Default.Home, "Welcome to Home"),
        Triple("Itineraries", Icons.Default.Favorite, "Your Itineraries Items"),
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
        composable("detail/{title}/{subtitle}/{imageUrl}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "No Title"
            val subtitle = backStackEntry.arguments?.getString("subtitle") ?: "No Subtitle"
            val imageUrl = backStackEntry.arguments?.getString("imageUrl")?.decode() ?: ""
            DetailScreen(title, subtitle, imageUrl, navController)
        }
        composable("profile") { ProfileActivity(navController) }
        composable("aiTripPlanner") { AITripPlannerScreen(navController) }
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
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Open Profile"
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
                    DestinationCard(
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
fun ItinerariesScreen(navController: NavController) {
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
                        Log.d("ItinerariesScreen", "Deserialized itinerary: $it")
                    }
                } catch (e: Exception) {
                    Log.e("ItinerariesScreen", "Failed to deserialize document ${doc.id}: ${e.message}", e)
                    null
                }
            }
            if (itineraries.isEmpty()) {
                Log.d("ItinerariesScreen", "No itineraries found for user $userId")
            }
        } catch (e: Exception) {
            errorMessage = "Error loading itineraries: ${e.message}"
            Log.e("ItinerariesScreen", "Error fetching itineraries: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Itineraries") }
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
                                .clickable {
                                    navController.navigate(
                                        "itineraryDetail/" +
                                                "${itinerary.destination.encode()}/" +
                                                "${itinerary.startDate.encode()}/" +
                                                "${itinerary.endDate.encode()}/" +
                                                "${itinerary.interests.joinToString(",").encode()}/" +
                                                "${itinerary.itineraryText.encode()}"
                                    )
                                },
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



@Preview(showBackground = true)
@Composable
fun BottomTabbedLayoutPreview() {
    NobiTheme {
        val navController = rememberNavController()
        MainNavigation(navController)
    }
}


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
