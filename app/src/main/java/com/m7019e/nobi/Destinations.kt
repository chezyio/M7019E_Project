package com.m7019e.nobi

data class Destination(
    val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String
)

val mockDestinations = listOf(
    Destination(
        title = "Paris",
        subtitle = "City of Light",
        description = "Explore the Eiffel Tower, Louvre Museum, and charming cafes along the Seine.",
        location = "France",
        imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&fm=jpg&w=1080&fit=max"
    ),
    Destination(
        title = "Tokyo",
        subtitle = "Vibrant Metropolis",
        description = "Experience Shibuya Crossing, ancient temples, and world-class sushi.",
        location = "Japan",
        imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?q=80&fm=jpg&w=1080&fit=max"
    ),
    Destination(
        title = "New York",
        subtitle = "The Big Apple",
        description = "Visit Times Square, Central Park, and the Statue of Liberty.",
        location = "USA",
        imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?q=80&fm=jpg&w=1080&fit=max"
    ),
    Destination(
        title = "Rome",
        subtitle = "Eternal City",
        description = "Discover the Colosseum, Roman Forum, and authentic Italian cuisine.",
        location = "Italy",
        imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?q=80&fm=jpg&w=1080&fit=max"
    )
)