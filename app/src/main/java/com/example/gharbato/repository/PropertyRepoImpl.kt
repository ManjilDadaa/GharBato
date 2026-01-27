package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.SimilarProperty
import com.example.gharbato.model.SimilarityWeights
import com.example.gharbato.repository.PropertyRepo
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

private const val TAG = "PropertyRepoImpl"

class PropertyRepoImpl : PropertyRepo {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ref: DatabaseReference = database.getReference("Property")

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    // ========== PROPERTY RETRIEVAL ==========

    override suspend fun getAllProperties(): List<PropertyModel> {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Fetching all properties from Firebase...")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val properties = mutableListOf<PropertyModel>()

                    Log.d(TAG, "Firebase snapshot has ${snapshot.childrenCount} children")

                    for (propertySnapshot in snapshot.children) {
                        try {
                            val property = propertySnapshot.getValue(PropertyModel::class.java)
                            if (property != null) {
                                properties.add(property)
                                Log.d(TAG, "✓ Loaded: ${property.title} - ${property.location} (${property.latitude}, ${property.longitude})")
                            } else {
                                Log.w(TAG, "✗ Null property at key: ${propertySnapshot.key}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "✗ Error parsing property at key: ${propertySnapshot.key}", e)
                        }
                    }

                    Log.d(TAG, "Successfully loaded ${properties.size} properties")
                    continuation.resume(properties)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                    continuation.resumeWithException(
                        Exception("Firebase error: ${error.message}")
                    )
                }
            })
        }
    }

    override suspend fun getAllApprovedProperties(): List<PropertyModel> {
        return suspendCancellableCoroutine { continuation ->

            ref.orderByChild("status")
                .equalTo(PropertyStatus.APPROVED)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val properties = mutableListOf<PropertyModel>()

                        for (propertySnapshot in snapshot.children) {
                            val property = propertySnapshot.getValue(PropertyModel::class.java)
                            if (property != null && property.propertyStatus == "AVAILABLE") {
                                properties.add(property)
                            }
                        }

                        continuation.resume(properties)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(
                            Exception(error.message)
                        )
                    }
                })
        }
    }


    override suspend fun getPropertyById(id: Int): PropertyModel? {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Fetching APPROVED property by ID: $id")

            // First, query by ID
            ref.orderByChild("id")
                .equalTo(id.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Filter only approved properties
                        val property = snapshot.children
                            .mapNotNull { it.getValue(PropertyModel::class.java) }
                            .firstOrNull { it.status == PropertyStatus.APPROVED } // Only approved

                        if (property != null) {
                            Log.d(TAG, "Found APPROVED property: ${property.title}")
                        } else {
                            Log.w(TAG, "No APPROVED property found with ID: $id")
                        }

                        continuation.resume(property)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase error: ${error.message}")
                        continuation.resumeWithException(
                            Exception("Firebase error: ${error.message}")
                        )
                    }
                })
        }
    }


    // ========== SEARCH & FILTER ==========

    override suspend fun searchProperties(query: String): List<PropertyModel> {
        Log.d(TAG, "Searching properties with query: '$query'")

        val allProperties = getAllApprovedProperties()
        Log.d(TAG, "Total properties to search: ${allProperties.size}")

        val result = if (query.isEmpty()) {
            Log.d(TAG, "Empty query - returning all properties")
            allProperties
        } else {
            val searchQuery = query.lowercase().trim()
            Log.d(TAG, "Normalized query: '$searchQuery'")

            val filtered = allProperties.filter { property ->
                val titleMatch = property.title.lowercase().contains(searchQuery)
                val locationMatch = property.location.lowercase().contains(searchQuery)
                val developerMatch = property.developer.lowercase().contains(searchQuery)
                val propertyTypeMatch = property.propertyType.lowercase().contains(searchQuery)
                val descriptionMatch = property.description?.lowercase()?.contains(searchQuery) ?: false
                // Map "Sell" -> "Buy" for user-facing text search
                val displayMarketType = if (property.marketType.equals("Sell", ignoreCase = true)) "buy" else property.marketType.lowercase()
                val marketTypeMatch = displayMarketType.contains(searchQuery)

                val matches = titleMatch || locationMatch || developerMatch ||
                        propertyTypeMatch || descriptionMatch || marketTypeMatch

                if (matches) {
                    Log.d(TAG, "✓ Match: ${property.title} (${property.location})")
                }

                matches
            }

            Log.d(TAG, "Search found ${filtered.size} matching properties")
            filtered
        }

        return result
    }

    override suspend fun filterProperties(filters: PropertyFilters): List<PropertyModel> {
        Log.d(TAG, "Filtering properties with: $filters")

        val allProperties = getAllApprovedProperties()
        var filtered = allProperties

        Log.d(TAG, "Starting with ${filtered.size} properties")

        // Market Type (Sell/Rent/Book)
        if (filters.marketType.isNotBlank()) {
            filtered = filtered.filter { property ->
                property.marketType.equals(filters.marketType, ignoreCase = true)
            }
            Log.d(TAG, "After market type filter: ${filtered.size} properties")
        }

        // Property Types
        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property type filter: ${filtered.size} properties")
        }

        // Price Range
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                val minPriceValue = filters.minPrice * 1000
                val maxPriceValue = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE

                priceValue >= minPriceValue && priceValue <= maxPriceValue
            }
            Log.d(TAG, "After price filter: ${filtered.size} properties")
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
            Log.d(TAG, "After bedrooms filter: ${filtered.size} properties")
        }

        // Furnishing
        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing filter: ${filtered.size} properties")
        }

        // Parking
        filters.parking?.let { parkingRequired ->
            filtered = filtered.filter { property ->
                property.parking == parkingRequired
            }
            Log.d(TAG, "After parking filter: ${filtered.size} properties")
        }

        // Pets Allowed
        filters.petsAllowed?.let { petsRequired ->
            filtered = filtered.filter { property ->
                property.petsAllowed == petsRequired
            }
            Log.d(TAG, "After pets filter: ${filtered.size} properties")
        }

        // Amenities (property must have ALL selected amenities)
        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities filter: ${filtered.size} properties")
        }

        // Floor
        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor filter: ${filtered.size} properties")
        }

        Log.d(TAG, "Final filtered result: ${filtered.size} properties")
        return filtered
    }

    // ========== LOCATION-BASED SEARCH ==========

    override suspend fun getPropertiesByLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel> {
        Log.d(TAG, "Searching by location - Lat: $latitude, Lng: $longitude, Radius: ${radiusKm}km")

        val allProperties = getAllApprovedProperties()
        Log.d(TAG, "Total properties to check: ${allProperties.size}")

        val filtered = allProperties.filter { property ->
            try {
                val distance = calculateDistance(
                    lat1 = latitude,
                    lon1 = longitude,
                    lat2 = property.latitude,
                    lon2 = property.longitude
                )

                val withinRadius = distance <= radiusKm

                Log.d(TAG, "${if (withinRadius) "✓" else "✗"} ${property.title} - " +
                        "${String.format("%.2f", distance)}km away " +
                        "(${property.latitude}, ${property.longitude})")

                withinRadius
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error calculating distance for ${property.title}: ${e.message}")
                false
            }
        }

        Log.d(TAG, "Location search found ${filtered.size} properties within ${radiusKm}km")
        return filtered
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return Distance in kilometers
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    /**
     * Extract numeric price value from price string
     * Example: "NPR 25,000/month" -> 25000
     * Example: "25000" -> 25000
     */
    private fun extractPriceValue(priceString: String): Int {
        val numbers = priceString.filter { it.isDigit() }
        val value = numbers.toIntOrNull() ?: 0
        Log.d(TAG, "Extracted price: '$priceString' -> $value")
        return value
    }

    // ========== PROPERTY MANAGEMENT ==========

    override fun addProperty(
        property: PropertyModel,
        callback: (Boolean, String?) -> Unit
    ) {
        val propertyId = ref.push().key

        if (propertyId == null) {
            callback(false, "Failed to generate ID")
            return
        }

        ref.child(propertyId)
            .setValue(property.copy(id = propertyId.hashCode(), firebaseKey = propertyId))
            .addOnSuccessListener {
                Log.d(TAG, "Property added successfully: ${property.title}")
                callback(true, null)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to add property: ${it.message}")
                callback(false, it.message)
            }
    }

    // ========== IMAGE UPLOAD ==========

    override fun uploadImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    ) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                var fileName = getFileNameFromUri(context, imageUri)
                fileName = fileName?.substringBeforeLast(".") ?: "uploaded_image"

                val response = cloudinary.uploader().upload(
                    inputStream, ObjectUtils.asMap(
                        "public_id", fileName,
                        "resource_type", "image"
                    )
                )

                var imageUrl = response["url"] as String?
                imageUrl = imageUrl?.replace("http://", "https://")

                Handler(Looper.getMainLooper()).post {
                    callback(imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback(null)
                }
            }
        }
    }

    override fun uploadMultipleImages(
        context: Context,
        imageUris: List<Uri>,
        callback: (List<String>) -> Unit
    ) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val uploadedUrls = mutableListOf<String>()

            try {
                imageUris.forEach { uri ->
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        var fileName = getFileNameFromUri(context, uri)
                        fileName = fileName?.substringBeforeLast(".")
                            ?: "image_${System.currentTimeMillis()}_${uploadedUrls.size}"

                        val response = cloudinary.uploader().upload(
                            inputStream, ObjectUtils.asMap(
                                "public_id", fileName,
                                "resource_type", "image",
                                "folder", "properties"
                            )
                        )

                        var imageUrl = response["url"] as String?
                        imageUrl = imageUrl?.replace("http://", "https://")

                        if (imageUrl != null) {
                            uploadedUrls.add(imageUrl)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Handler(Looper.getMainLooper()).post {
                callback(uploadedUrls)
            }
        }
    }

    override fun getFileNameFromUri(
        context: Context,
        imageUri: Uri
    ): String? {
        val cursor = context.contentResolver.query(
            imageUri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                return it.getString(nameIndex)
            }
        }

        return "image_${System.currentTimeMillis()}"
    }


// this is the scoring criteria to show
    /**
     * Get similar properties based on multiple factors with weighted scoring
     *
     * Scoring Algorithm:
     * 1. Market Type Match (30 points) - Same rent/sale/book type
     * 2. Property Type Match (20 points) - Same apartment/house/land
     * 3. Price Similarity (15 points) - Within ±30% price range
     * 4. Location Match (15 points) - Same city/area or within 5km
     * 5. Bedroom Match (10 points) - Same or ±1 bedroom
     * 6. Area Similarity (5 points) - Within ±20% size
     * 7. Bathroom Match (3 points) - Same or ±1 bathroom
     * 8. Furnishing Match (2 points) - Same furnishing type
     *
     * Total possible score: 100 points
     */
    override suspend fun getSimilarProperties(
        currentProperty: PropertyModel,
        limit: Int
    ): List<PropertyModel> {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Finding similar properties for: ${currentProperty.title}")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allProperties = mutableListOf<PropertyModel>()

                    // Get all approved properties
                    for (propertySnapshot in snapshot.children) {
                        try {
                            val property = propertySnapshot.getValue(PropertyModel::class.java)
                            if (property != null &&
                                property.status == PropertyStatus.APPROVED &&
                                property.propertyStatus == "AVAILABLE" &&
                                property.id != currentProperty.id) { // Exclude current property
                                allProperties.add(property)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing property: ${e.message}")
                        }
                    }

                    // Calculate similarity scores for each property
                    val scoredProperties = allProperties.map { property ->
                        val score = calculateSimilarityScore(currentProperty, property)
                        SimilarProperty(property, score)
                    }

                    // Sort by score (highest first) and take top results
                    val similarProperties = scoredProperties
                        .filter { it.similarityScore > 20f } // Minimum 20% similarity
                        .sortedByDescending { it.similarityScore }
                        .take(limit)
                        .map { it.property }

                    Log.d(TAG, "Found ${similarProperties.size} similar properties")
                    continuation.resume(similarProperties)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                    continuation.resume(emptyList()) // Return empty list on error
                }
            })
        }
    }

    /**
     * Calculate similarity score between two properties
     * Returns a score from 0-100 based on multiple weighted factors
     */
    private fun calculateSimilarityScore(
        current: PropertyModel,
        candidate: PropertyModel
    ): Float {
        var score = 0f

        // 1. Market Type Match (30 points) - CRITICAL
        if (current.marketType.equals(candidate.marketType, ignoreCase = true)) {
            score += SimilarityWeights.MARKET_TYPE
        }

        // 2. Property Type Match (20 points)
        if (current.propertyType.equals(candidate.propertyType, ignoreCase = true)) {
            score += SimilarityWeights.PROPERTY_TYPE
        }

        // 3. Price Similarity (15 points)
        val priceScore = calculatePriceSimilarity(current.price, candidate.price)
        score += priceScore * SimilarityWeights.PRICE_RANGE

        // 4. Location Match (15 points)
        val locationScore = calculateLocationSimilarity(
            current.location, candidate.location,
            current.latitude, current.longitude,
            candidate.latitude, candidate.longitude
        )
        score += locationScore * SimilarityWeights.LOCATION

        // 5. Bedroom Match (10 points)
        val bedroomScore = calculateBedroomSimilarity(current.bedrooms, candidate.bedrooms)
        score += bedroomScore * SimilarityWeights.BEDROOMS

        // 6. Area Similarity (5 points)
        val areaScore = calculateAreaSimilarity(current.sqft, candidate.sqft)
        score += areaScore * SimilarityWeights.AREA

        // 7. Bathroom Match (3 points)
        val bathroomScore = calculateBathroomSimilarity(current.bathrooms, candidate.bathrooms)
        score += bathroomScore * SimilarityWeights.BATHROOMS

        // 8. Furnishing Match (2 points)
        if (current.furnishing.equals(candidate.furnishing, ignoreCase = true)) {
            score += SimilarityWeights.FURNISHING
        }

        return score
    }

    /**
     * Calculate price similarity score (0.0 to 1.0)
     * Returns 1.0 for exact match, decreasing as prices differ
     * Returns 0.0 if prices differ by more than 30%
     */
    private fun calculatePriceSimilarity(price1: String, price2: String): Float {
        try {
            // Extract numeric values from price strings (e.g., "Rs 15000/month" -> 15000)
            val num1 = price1.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return 0f
            val num2 = price2.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return 0f

            if (num1 == 0.0 || num2 == 0.0) return 0f

            val ratio = min(num1, num2) / max(num1, num2)

            // If within 30% range, give high score
            return when {
                ratio >= 0.9 -> 1.0f  // Within 10% = perfect
                ratio >= 0.8 -> 0.8f  // Within 20% = good
                ratio >= 0.7 -> 0.5f  // Within 30% = okay
                else -> 0f            // More than 30% difference
            }
        } catch (e: Exception) {
            return 0f
        }
    }

    /**
     * Calculate location similarity score (0.0 to 1.0)
     * Checks both string match and geographic distance
     */
    private fun calculateLocationSimilarity(
        loc1: String, loc2: String,
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        // Check if location strings match (city/area)
        val locationMatch = when {
            loc1.equals(loc2, ignoreCase = true) -> 1.0f
            loc1.contains(loc2, ignoreCase = true) || loc2.contains(loc1, ignoreCase = true) -> 0.7f
            else -> 0f
        }

        // Calculate geographic distance
        val distance = calculateDistance(lat1, lng1, lat2, lng2)
        val distanceScore = when {
            distance <= 1.0 -> 1.0f   // Within 1km = perfect
            distance <= 3.0 -> 0.7f   // Within 3km = good
            distance <= 5.0 -> 0.4f   // Within 5km = okay
            else -> 0f                // More than 5km
        }

        // Return the better of the two scores
        return max(locationMatch, distanceScore)
    }

    /**
     * Calculate bedroom similarity score (0.0 to 1.0)
     */
    private fun calculateBedroomSimilarity(bed1: Int, bed2: Int): Float {
        return when (abs(bed1 - bed2)) {
            0 -> 1.0f    // Exact match
            1 -> 0.6f    // ±1 bedroom
            2 -> 0.3f    // ±2 bedrooms
            else -> 0f   // Too different
        }
    }

    /**
     * Calculate bathroom similarity score (0.0 to 1.0)
     */
    private fun calculateBathroomSimilarity(bath1: Int, bath2: Int): Float {
        return when (abs(bath1 - bath2)) {
            0 -> 1.0f    // Exact match
            1 -> 0.5f    // ±1 bathroom
            else -> 0f   // Too different
        }
    }

    /**
     * Calculate area similarity score (0.0 to 1.0)
     * Returns high score if areas are within 20% of each other
     */
    private fun calculateAreaSimilarity(sqft1: String, sqft2: String): Float {
        try {
            val area1 = sqft1.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return 0f
            val area2 = sqft2.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return 0f

            if (area1 == 0.0 || area2 == 0.0) return 0f

            val ratio = min(area1, area2) / max(area1, area2)

            return when {
                ratio >= 0.9 -> 1.0f  // Within 10%
                ratio >= 0.8 -> 0.6f  // Within 20%
                else -> 0f            // More than 20% difference
            }
        } catch (e: Exception) {
            return 0f
        }
    }
}