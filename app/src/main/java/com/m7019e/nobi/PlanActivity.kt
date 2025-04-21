package com.m7019e.nobi

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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
                    val prompt = "Create a $days-day itinerary for $destination from $startDate to $endDate, focusing on ${interests.joinToString(", ")}. Structure each day as ‘Day X: [Theme/Highlights]’ and divide the activities into sections: ‘Morning,’ ‘Afternoon,’ and ‘Evening.’ Group each activity/location with a title, description, and nothing else."
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
