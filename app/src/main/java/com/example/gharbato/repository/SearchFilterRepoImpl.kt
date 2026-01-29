package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyFilters
import kotlin.math.*
private const val TAG = "SearchFilterRepo"

class SearchFilterRepoImpl : SearchFilterRepo {

    override suspend fun searchByText(
        properties: List<PropertyModel>,
        query: String
    ): List<PropertyModel> {
        if (query.isEmpty()) return properties

        val searchQuery = query.lowercase().trim()
        Log.d(TAG, "Searching text: '$searchQuery' in ${properties.size} properties")

        val filtered = properties.filter { property ->
            // Map "Sell" -> "Buy" for user-facing text search
            val displayMarketType = if (property.marketType.equals("Sell", ignoreCase = true)) "buy" else property.marketType.lowercase()
            property.title.lowercase().contains(searchQuery) ||
                    property.location.lowercase().contains(searchQuery) ||
                    property.developer.lowercase().contains(searchQuery) ||
                    property.propertyType.lowercase().contains(searchQuery) ||
                    displayMarketType.contains(searchQuery) ||
                    property.description?.lowercase()?.contains(searchQuery) == true
        }

        Log.d(TAG, "Text search found ${filtered.size} matches")
        return filtered
    }

    override suspend fun filterByLocation(
        properties: List<PropertyModel>,
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel> {
        Log.d(TAG, "Filtering by location: ($latitude, $longitude) within ${radiusKm}km")

        val filtered = properties.filter { property ->
            val distance = calculateDistance(
                latitude, longitude,
                property.latitude, property.longitude
            )
            distance <= radiusKm
        }

        Log.d(TAG, "Location filter found ${filtered.size} properties")
        return filtered
    }

    override suspend fun applyFilters(
        properties: List<PropertyModel>,
        filters: PropertyFilters
    ): List<PropertyModel> {
        var filtered = properties
        Log.d(TAG, "Applying filters to ${properties.size} properties: $filters")

        // Market Type (Sell/Rent/Book)
        if (filters.marketType.isNotBlank()) {
            filtered = filtered.filter { property ->
                property.marketType.equals(filters.marketType, ignoreCase = true)
            }
            Log.d(TAG, "After market type: ${filtered.size}")
        }

        // Property Types
        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property types: ${filtered.size}")
        }

        // Price Range
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                val minPriceValue = filters.minPrice * 1000
                val maxPriceValue = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                priceValue in minPriceValue..maxPriceValue
            }
            Log.d(TAG, "After price range: ${filtered.size}")
        }

        // Bedrooms
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

        // Furnishing
        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing: ${filtered.size}")
        }

        // Parking
        filters.parking?.let { parkingRequired ->
            filtered = filtered.filter { it.parking == parkingRequired }
            Log.d(TAG, "After parking: ${filtered.size}")
        }

        // Pets Allowed
        filters.petsAllowed?.let { petsRequired ->
            filtered = filtered.filter { it.petsAllowed == petsRequired }
            Log.d(TAG, "After pets: ${filtered.size}")
        }

        // Amenities (must have ALL selected amenities)
        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities: ${filtered.size}")
        }

        // Floor
        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor: ${filtered.size}")
        }

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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }

    private fun extractPriceValue(priceString: String): Int {
        val numbers = priceString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }
}