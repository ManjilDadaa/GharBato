package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.model.PropertyFilters
import kotlin.math.*

private const val TAG = "SearchFilterRepoImpl"

class SearchFilterRepoImpl : SearchFilterRepo {

    override suspend fun searchByText(
        properties: List<PropertyModel>,
        query: String
    ): List<PropertyModel> {
        if (query.isEmpty()) {
            return properties
        }

        val searchQuery = query.lowercase().trim()
        Log.d(TAG, "Searching with query: '$searchQuery' in ${properties.size} properties")

        val results = properties.filter { property ->
            property.title.lowercase().contains(searchQuery) ||
                    property.location.lowercase().contains(searchQuery) ||
                    property.developer.lowercase().contains(searchQuery) ||
                    property.propertyType.lowercase().contains(searchQuery) ||
                    property.description?.lowercase()?.contains(searchQuery) == true
        }

        Log.d(TAG, "Search found ${results.size} properties")
        return results
    }

    override suspend fun filterByLocation(
        properties: List<PropertyModel>,
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel> {
        Log.d(TAG, "Filtering by location: ($latitude, $longitude), radius: ${radiusKm}km")

        val results = properties.filter { property ->
            val distance = calculateDistance(
                latitude, longitude,
                property.latitude, property.longitude
            )
            distance <= radiusKm
        }

        Log.d(TAG, "Location filter found ${results.size} properties")
        return results
    }

    override suspend fun applyFilters(
        properties: List<PropertyModel>,
        filters: PropertyFilters
    ): List<PropertyModel> {
        var filtered = properties
        Log.d(TAG, "Applying filters to ${properties.size} properties")

        if (filters.marketType.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.marketType.equals(filters.marketType, ignoreCase = true)
            }
            Log.d(TAG, "After market type (${filters.marketType}): ${filtered.size}")
        }

        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property types: ${filtered.size}")
        }

        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val price = extractPriceValue(property.price)
                val min = filters.minPrice * 1000
                val max = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                price in min..max
            }
            Log.d(TAG, "After price range: ${filtered.size}")
        }

        if (filters.bedrooms.isNotEmpty()) {
            filtered = filtered.filter { property ->
                when (filters.bedrooms) {
                    "Studio" -> property.bedrooms == 0
                    "6+" -> property.bedrooms >= 6
                    else -> property.bedrooms == filters.bedrooms.toIntOrNull()
                }
            }
            Log.d(TAG, "After bedrooms: ${filtered.size}")
        }

        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing: ${filtered.size}")
        }

        filters.parking?.let { required ->
            filtered = filtered.filter { it.parking == required }
            Log.d(TAG, "After parking: ${filtered.size}")
        }

        filters.petsAllowed?.let { required ->
            filtered = filtered.filter { it.petsAllowed == required }
            Log.d(TAG, "After pets: ${filtered.size}")
        }

        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities: ${filtered.size}")
        }

        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor: ${filtered.size}")
        }

        Log.d(TAG, "Final filtered result: ${filtered.size} properties")
        return filtered
    }

    override fun hasActiveFilters(filters: PropertyFilters): Boolean {
        return filters.propertyTypes.isNotEmpty() ||
                filters.minPrice > 0 ||
                filters.maxPrice > 0 ||
                filters.bedrooms.isNotEmpty() ||
                filters.furnishing.isNotEmpty() ||
                filters.parking != null ||
                filters.petsAllowed != null ||
                filters.amenities.isNotEmpty() ||
                filters.floor.isNotEmpty()
    }

    override fun extractPriceValue(priceString: String): Int {
        val numbers = priceString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }

    override fun extractAreaValue(areaString: String): Int {
        val numbers = areaString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}