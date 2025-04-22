package com.m7019e.nobi

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileActivity(navController: NavController) {
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
                        delay(300)
                        navController.popBackStack()
                    }
                },
                onLogout = {
                    scope.launch {
                        FirebaseAuth.getInstance().signOut()
                        isVisible = false
                        delay(300)
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
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
        TopAppBar(
            title = { Text("Profile") }
        )

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

        Text(
            text = "Debug logs...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

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
