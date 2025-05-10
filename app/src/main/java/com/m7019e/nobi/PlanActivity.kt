package com.m7019e.nobi

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(navController: NavController, viewModel: ItinerariesViewModel = viewModel()) {
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(5)) }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var itinerary by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showItineraryDialog by remember { mutableStateOf(false) }
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
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    fun generateItinerary() {
        if (destination.isNotBlank() && interests.isNotEmpty() && !endDate.isBefore(startDate)) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val days = endDate.toEpochDay() - startDate.toEpochDay() + 1
                    val prompt = "Create a $days-day itinerary for $destination from $startDate to $endDate, focusing on ${interests.joinToString(", ")}. Structure each day as ‘Day X: [Theme/Highlights]’ and divide the activities into sections: ‘Morning,’ ‘Afternoon,’ and ‘Evening.’ Group each activity/location with a title, description, and nothing else."
                    Log.d("PlanScreen", "Generating with prompt: $prompt")
                    val response = generativeModel.generateContent(prompt)
                    itinerary = response.text ?: "No itinerary generated."
                    Log.d("PlanScreen", "Response: $itinerary")
                    showItineraryDialog = true
                } catch (e: Exception) {
                    itinerary = "Error generating itinerary: ${e.message}"
                    Log.e("PlanScreen", "Error: ${e.message}", e)
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
    Log.d("PlanScreen", "Raw itinerary: '$itinerary'")
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
    Log.d("PlanScreen", "Parsed intro: '$introText'")
    Log.d("PlanScreen", "Parsed plans: $dayPlans")
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
    private val db = FirebaseFirestore.getInstance()
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