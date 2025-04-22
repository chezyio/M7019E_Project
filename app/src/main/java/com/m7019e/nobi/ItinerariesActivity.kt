package com.m7019e.nobi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItinerariesScreen(navController: NavController, viewModel: ItinerariesViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
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
            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (viewModel.itineraries.isEmpty()) {
                    item {
                        Text(
                            text = "No saved itineraries. Plan a trip with AI!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(viewModel.itineraries, key = { it.id }) { itineraryWithId ->
                        ItineraryCard(
                            itinerary = itineraryWithId.itinerary,
                            onClick = {
                                navController.navigate(
                                    "itineraryDetail/" +
                                            "${itineraryWithId.itinerary.destination.encode()}/" +
                                            "${itineraryWithId.itinerary.startDate.encode()}/" +
                                            "${itineraryWithId.itinerary.endDate.encode()}/" +
                                            "${itineraryWithId.itinerary.interests.joinToString(",").encode()}/" +
                                            "${itineraryWithId.itinerary.itineraryText.encode()}"
                                )
                            },
                            onDelete = {
                                viewModel.deleteItinerary(itineraryWithId.id)
                            }
                        )
                    }
                }
            }
        }
    }
}