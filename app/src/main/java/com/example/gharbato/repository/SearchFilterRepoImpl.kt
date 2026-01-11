package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.PropertyModel
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
        Log.d(TAG, "=== APPLYING FILTERS ===")
        Log.d(TAG, "Starting with ${properties.size} properties")
        Log.d(TAG, "Filters: $filters")

        if (!hasActiveFilters(filters) && filters.marketType.isEmpty()) {
            Log.d(TAG, "No active filters - returning all ${properties.size} properties")
            return properties
        }

        // Market Type Filter
        if (filters.marketType.isNotEmpty()) {
            Log.d(TAG, "Filtering by marketType: '${filters.marketType}'")

            properties.take(3).forEach { property ->
                Log.d(TAG, "Sample property ${property.id} marketType: '${property.marketType}'")
            }

            filtered = filtered.filter { property ->
                val matches = property.marketType.equals(filters.marketType, ignoreCase = true)
                if (!matches) {
                    Log.v(TAG, "Property ${property.id} marketType '${property.marketType}' != '${filters.marketType}'")
                }
                matches
            }
            Log.d(TAG, "After market type (${filters.marketType}): ${filtered.size} properties")

            if (filtered.isEmpty()) {
                Log.w(TAG, "⚠️ NO PROPERTIES MATCH marketType '${filters.marketType}'")
                Log.w(TAG, "Available marketTypes in database:")
                properties.map { it.marketType }.distinct().forEach { type ->
                    Log.w(TAG, "  - '$type'")
                }
            }
        }

        // Property Types Filter
        if (filters.propertyTypes.isNotEmpty()) {
            Log.d(TAG, "Filtering by property types: ${filters.propertyTypes}")
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property types: ${filtered.size} properties")
        }

        // Price Range Filter
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            Log.d(TAG, "Filtering by price: ${filters.minPrice}k - ${filters.maxPrice}k")
            filtered = filtered.filter { property ->
                val price = extractPriceValue(property.price)
                val min = filters.minPrice * 1000
                val max = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                val inRange = price in min..max
                if (!inRange) {
                    Log.v(TAG, "Property ${property.id} price $price not in range [$min, $max]")
                }
                inRange
            }
            Log.d(TAG, "After price range: ${filtered.size} properties")
        }

        // Bedrooms Filter
        if (filters.bedrooms.isNotEmpty()) {
            Log.d(TAG, "Filtering by bedrooms: '${filters.bedrooms}'")
            filtered = filtered.filter { property ->
                when (filters.bedrooms) {
                    "Studio" -> property.bedrooms == 0
                    "6+" -> property.bedrooms >= 6
                    else -> property.bedrooms == filters.bedrooms.toIntOrNull()
                }
            }
            Log.d(TAG, "After bedrooms: ${filtered.size} properties")
        }

        // Furnishing Filter
        if (filters.furnishing.isNotEmpty()) {
            Log.d(TAG, "Filtering by furnishing: '${filters.furnishing}'")
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing: ${filtered.size} properties")
        }

        // Parking Filter
        filters.parking?.let { required ->
            Log.d(TAG, "Filtering by parking: $required")
            filtered = filtered.filter { it.parking == required }
            Log.d(TAG, "After parking: ${filtered.size} properties")
        }

        // Pets Allowed Filter
        filters.petsAllowed?.let { required ->
            Log.d(TAG, "Filtering by pets: $required")
            filtered = filtered.filter { it.petsAllowed == required }
            Log.d(TAG, "After pets: ${filtered.size} properties")
        }

        // Amenities Filter
        if (filters.amenities.isNotEmpty()) {
            Log.d(TAG, "Filtering by amenities: ${filters.amenities}")
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities: ${filtered.size} properties")
        }

        // Floor Filter
        if (filters.floor.isNotEmpty()) {
            Log.d(TAG, "Filtering by floor: '${filters.floor}'")
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor: ${filtered.size} properties")
        }

        Log.d(TAG, "=== FILTER RESULT: ${filtered.size} properties ===")
        return filtered
    }

    override fun hasActiveFilters(filters: PropertyFilters): Boolean {
        val hasFilters = filters.propertyTypes.isNotEmpty() ||
                filters.minPrice > 0 ||
                filters.maxPrice > 0 ||
                filters.bedrooms.isNotEmpty() ||
                filters.furnishing.isNotEmpty() ||
                filters.parking != null ||
                filters.petsAllowed != null ||
                filters.amenities.isNotEmpty() ||
                filters.floor.isNotEmpty()

        Log.d(TAG, "hasActiveFilters: $hasFilters (marketType not counted here)")
        return hasFilters
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