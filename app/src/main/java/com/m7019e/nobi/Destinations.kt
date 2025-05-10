package com.m7019e.nobi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@kotlinx.serialization.Serializable
data class Destination(
    val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String
)

@Composable
fun rememberDestinations(viewModel: DestinationsViewModel = viewModel()): DestinationsState {
    return viewModel.destinationsState.collectAsState().value
}

sealed class DestinationsState {
    object Loading : DestinationsState()
    data class Success(val destinations: List<Destination>) : DestinationsState()
    data class Error(val message: String) : DestinationsState()
}