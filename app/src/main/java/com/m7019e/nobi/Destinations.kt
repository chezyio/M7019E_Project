package com.m7019e.nobi

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "destinations")
data class Destination(
    @PrimaryKey val title: String,
    val subtitle: String,
    val description: String,
    val location: String,
    val imageUrl: String,
    val cachePath: String? = null
)

var destinations: List<Destination> = emptyList()