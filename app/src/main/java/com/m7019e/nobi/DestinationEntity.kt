package com.m7019e.nobi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String
)

fun DestinationEntity.toDestination(): Destination {
    return Destination(
        title = title,
        subtitle = subtitle,
        description = description,
        location = location,
        imageUrl = imageUrl
    )
}

fun Destination.toEntity(): DestinationEntity {
    return DestinationEntity(
        title = title,
        subtitle = subtitle,
        description = description,
        location = location,
        imageUrl = imageUrl
    )
}