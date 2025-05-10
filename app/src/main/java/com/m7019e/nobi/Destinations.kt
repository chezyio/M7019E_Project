package com.m7019e.nobi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Destination(
    val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String
)

@Composable
fun rememberDestinations(): DestinationsState {
    var state by remember { mutableStateOf<DestinationsState>(DestinationsState.Loading) }

    LaunchedEffect(Unit) {
        state = try {
            val destinations = withContext(Dispatchers.IO) {
                CountryApiService.fetchTenCountries()
            }
            DestinationsState.Success(destinations)
        } catch (e: Exception) {
            DestinationsState.Error("Failed to load destinations: ${e.message}")
        }
    }

    return state
}

sealed class DestinationsState {
    object Loading : DestinationsState()
    data class Success(val destinations: List<Destination>) : DestinationsState()
    data class Error(val message: String) : DestinationsState()
}