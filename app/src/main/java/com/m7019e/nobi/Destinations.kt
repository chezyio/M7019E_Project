package com.m7019e.nobi

import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String,
    val cachePath: String? = null
)

// Global list to hold destinations fetched from API
var destinations: List<Destination> = emptyList()