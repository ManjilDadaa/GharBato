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
                            if (property != null) {
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
                val marketTypeMatch = property.marketType.lowercase().contains(searchQuery)

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

        // Market Type (Buy/Rent/Book)
        filtered = filtered.filter { property ->
            property.marketType.equals(filters.marketType, ignoreCase = true)
        }
        Log.d(TAG, "After market type filter: ${filtered.size} properties")

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
            .setValue(property.copy(id = propertyId.hashCode()))
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
}