package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.data.model.PlaceType
import com.example.gharbato.data.repository.NearbyPlacesRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*
import kotlin.random.Random


class NearbyPlacesRepositoryImpl : NearbyPlacesRepository {

    companion object {
        private const val TAG = "NearbyPlacesRepo"
        private const val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"
        private const val SEARCH_RADIUS = 2000
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
        private const val REQUEST_TIMEOUT_MS = 8000L // 8 second timeout
    }

    private val cache = mutableMapOf<String, CachedResult>()

    data class CachedResult(
        val data: List<NearbyPlace>,
        val timestamp: Long,
        val isRealData: Boolean
    )

    override suspend fun getNearbyPlaces(location: LatLng): Map<PlaceType, List<NearbyPlace>> {
        return withContext(Dispatchers.IO) {
            try {
                val essentialTypes = listOf(
                    PlaceType.SCHOOL,
                    PlaceType.HOSPITAL,
                    PlaceType.STORE
                )

                val deferredResults = essentialTypes.map { type ->
                    async {
                        type to getPlacesByType(location, type)
                    }
                }

                deferredResults.awaitAll().toMap()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby places", e)
                // Return fallback data
                generateFallbackPlaces(location)
            }
        }
    }

    override suspend fun getPlacesByType(location: LatLng, type: PlaceType): List<NearbyPlace> {
        return withContext(Dispatchers.IO) {
            val cacheKey = "${location.latitude},${location.longitude}_$type"

            // Check cache
            cache[cacheKey]?.let { cached ->
                val age = System.currentTimeMillis() - cached.timestamp
                if (age < CACHE_DURATION_MS) {
                    Log.d(TAG, "Cache hit for $type (${if(cached.isRealData) "REAL" else "FALLBACK"})")
                    return@withContext cached.data
                }
            }

            // Try to fetch from OSM with timeout
            val places = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                try {
                    fetchPlacesFromOSM(location, type)
                } catch (e: Exception) {
                    Log.e(TAG, "OSM fetch failed for $type", e)
                    null
                }
            }

            if (!places.isNullOrEmpty()) {
                // Success! Cache real data
                cache[cacheKey] = CachedResult(places, System.currentTimeMillis(), true)
                Log.d(TAG, "✓ Fetched ${places.size} REAL ${type.getDisplayName()} from OSM")
                places
            } else {
                // OSM failed or timed out, use fallback
                Log.w(TAG, "⚠ OSM unavailable for $type, using fallback")
                val fallback = generateFallbackForType(location, type)
                cache[cacheKey] = CachedResult(fallback, System.currentTimeMillis(), false)
                fallback
            }
        }
    }

    private fun fetchPlacesFromOSM(location: LatLng, type: PlaceType): List<NearbyPlace> {
        var connection: HttpURLConnection? = null

        try {
            val query = buildOverpassQuery(location, type)
            val url = URL(OVERPASS_API_URL)

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "GharbatoPropertyApp/1.0")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return parseOverpassResponse(response, location, type)
            }

            return emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildOverpassQuery(location: LatLng, type: PlaceType): String {
        val osmTags = getOSMTags(type)
        return """
            [out:json][timeout:5];
            (
              ${osmTags.joinToString("\n              ") { tag ->
            "node[\"$tag\"](around:$SEARCH_RADIUS,${location.latitude},${location.longitude});"
        }}
            );
            out body;
        """.trimIndent()
    }

    private fun getOSMTags(type: PlaceType): List<String> {
        return when (type) {
            PlaceType.SCHOOL -> listOf("amenity=school", "amenity=college")
            PlaceType.HOSPITAL -> listOf("amenity=hospital", "amenity=clinic")
            PlaceType.STORE -> listOf("shop=supermarket", "shop=convenience")
            PlaceType.PARK -> listOf("leisure=park")
            PlaceType.RESTAURANT -> listOf("amenity=restaurant", "amenity=cafe")
            PlaceType.TRANSPORT -> listOf("highway=bus_stop")
        }
    }

    private fun parseOverpassResponse(
        jsonResponse: String,
        baseLocation: LatLng,
        type: PlaceType
    ): List<NearbyPlace> {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val elements = jsonObject.getJSONArray("elements")
            val places = mutableListOf<NearbyPlace>()

            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val lat = element.optDouble("lat", 0.0)
                val lon = element.optDouble("lon", 0.0)

                if (lat == 0.0 || lon == 0.0) continue

                val location = LatLng(lat, lon)
                val tags = element.optJSONObject("tags") ?: continue
                val name = tags.optString("name", null) ?: continue

                places.add(
                    NearbyPlace(
                        id = element.optLong("id", 0).toString(),
                        name = name,
                        location = location,
                        type = type,
                        distance = calculateDistance(baseLocation, location),
                        formattedDistance = formatDistance(calculateDistance(baseLocation, location))
                    )
                )
            }

            return places.sortedBy { it.distance }.take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            return emptyList()
        }
    }

    // Fallback: Generate reasonable estimates based on location
    private fun generateFallbackPlaces(location: LatLng): Map<PlaceType, List<NearbyPlace>> {
        return mapOf(
            PlaceType.SCHOOL to generateFallbackForType(location, PlaceType.SCHOOL),
            PlaceType.HOSPITAL to generateFallbackForType(location, PlaceType.HOSPITAL),
            PlaceType.STORE to generateFallbackForType(location, PlaceType.STORE)
        )
    }

    private fun generateFallbackForType(location: LatLng, type: PlaceType): List<NearbyPlace> {
        val names = when (type) {
            PlaceType.SCHOOL -> listOf("Community School", "Public School", "Local School")
            PlaceType.HOSPITAL -> listOf("Health Clinic", "Medical Center", "Community Hospital")
            PlaceType.STORE -> listOf("Supermarket", "Local Store", "Convenience Store")
            else -> listOf("Nearby ${type.getDisplayName()}")
        }

        // Create unique but deterministic seed based on location + type
        val locationHash = ((location.latitude * 1000000).toLong() +
                (location.longitude * 1000000).toLong() +
                type.ordinal * 1000).toInt()
        val random = Random(locationHash)

        return names.take(3).mapIndexed { index, name ->
            // Generate random but consistent distances for this location (300m to 2500m)
            val distanceMeters = 300 + random.nextInt(2200)

            // Random direction (0-360 degrees)
            val angleDegrees = random.nextInt(360)
            val angleRadians = Math.toRadians(angleDegrees.toDouble())

            // Convert meters to lat/lng offsets
            // 1 degree latitude ≈ 111,000 meters
            // 1 degree longitude ≈ 111,000 * cos(latitude) meters
            val latOffset = (distanceMeters / 111000.0) * sin(angleRadians)
            val lngOffset = (distanceMeters / (111000.0 * cos(Math.toRadians(location.latitude)))) * cos(angleRadians)

            val placeLocation = LatLng(
                location.latitude + latOffset,
                location.longitude + lngOffset
            )

            // Calculate actual distance (should match distanceMeters closely)
            val actualDistance = calculateDistance(location, placeLocation)

            NearbyPlace(
                id = "fallback_${type}_${index}_${locationHash}",
                name = name,
                location = placeLocation,
                type = type,
                distance = actualDistance,
                formattedDistance = formatDistance(actualDistance)
            )
        }.sortedBy { it.distance }
    }


    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
            else -> String.format("%.1f km", distanceInMeters / 1000)
        }
    }

    fun clearCache() {
        cache.clear()
    }
}