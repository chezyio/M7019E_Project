package com.m7019e.nobi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class CountryApiResponse(
    @SerialName("name") val name: Name,
    @SerialName("capital") val capital: List<String>? = null,
    @SerialName("region") val region: String? = null,
    @SerialName("flags") val flags: Flags,
    @SerialName("population") val population: Long? = null
)

@Serializable
data class Name(
    @SerialName("common") val common: String
)

@Serializable
data class Flags(
    @SerialName("png") val png: String
)

object CountryApiService {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchTenCountries(): List<Destination> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://restcountries.com/v3.1/all")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            val countries = json.decodeFromString<List<CountryApiResponse>>(responseBody)

            countries.take(10).map { country ->
                Destination(
                    title = country.name.common,
                    subtitle = country.capital?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Explore ${country.name.common}",
                    description = "Discover ${country.name.common}, a vibrant destination in ${country.region ?: "the world"}.",
                    location = country.region ?: "Unknown",
                    imageUrl = country.flags.png
                )
            }
        }
    }
}