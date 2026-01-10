package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.model.PropertyStatus
import com.example.gharbato.model.PropertyFilters
import com.google.firebase.auth.FirebaseAuth
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
    private val usersRef: DatabaseReference = database.getReference("Users")
    private val notificationsRef: DatabaseReference = database.getReference("Notifications")
    private val auth = FirebaseAuth.getInstance()

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
                                Log.d(TAG, "✓ Loaded: ${property.title} - ${property.location}")
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
                        continuation.resumeWithException(Exception(error.message))
                    }
                })
        }
    }

    override suspend fun getPropertyById(id: Int): PropertyModel? {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Fetching APPROVED property by ID: $id")

            ref.orderByChild("id")
                .equalTo(id.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var property: PropertyModel? = null

                        for (child in snapshot.children) {
                            val prop = child.getValue(PropertyModel::class.java)
                            if (prop?.status == PropertyStatus.APPROVED) {
                                property = prop
                                break
                            }
                        }

                        if (property != null) {
                            Log.d(TAG, "Found APPROVED property: ${property.title}")
                        } else {
                            Log.w(TAG, "No APPROVED property found with ID: $id")
                        }
                        continuation.resume(property)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase error: ${error.message}")
                        continuation.resumeWithException(Exception("Firebase error: ${error.message}"))
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
            allProperties
        } else {
            val searchQuery = query.lowercase().trim()
            allProperties.filter { property ->
                property.title.lowercase().contains(searchQuery) ||
                        property.location.lowercase().contains(searchQuery) ||
                        property.developer.lowercase().contains(searchQuery) ||
                        property.propertyType.lowercase().contains(searchQuery) ||
                        property.description?.lowercase()?.contains(searchQuery) == true ||
                        property.marketType.lowercase().contains(searchQuery)
            }
        }

        Log.d(TAG, "Search found ${result.size} matching properties")
        return result
    }

    override suspend fun filterProperties(filters: PropertyFilters): List<PropertyModel> {
        Log.d(TAG, "Filtering properties with: $filters")
        val allProperties = getAllApprovedProperties()
        var filtered = allProperties

        filtered = filtered.filter { it.marketType.equals(filters.marketType, ignoreCase = true) }

        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { it.equals(property.propertyType, ignoreCase = true) }
            }
        }

        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                val minPriceValue = filters.minPrice * 1000
                val maxPriceValue = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                priceValue >= minPriceValue && priceValue <= maxPriceValue
            }
        }

        if (filters.bedrooms.isNotEmpty()) {
            filtered = filtered.filter { property ->
                when (filters.bedrooms) {
                    "Studio" -> property.bedrooms == 0
                    "6+" -> property.bedrooms >= 6
                    else -> property.bedrooms == filters.bedrooms.toIntOrNull()
                }
            }
        }

        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { it.furnishing.equals(filters.furnishing, ignoreCase = true) }
        }

        filters.parking?.let { parkingRequired ->
            filtered = filtered.filter { it.parking == parkingRequired }
        }

        filters.petsAllowed?.let { petsRequired ->
            filtered = filtered.filter { it.petsAllowed == petsRequired }
        }

        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
        }

        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { it.floor.equals(filters.floor, ignoreCase = true) }
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
        val allProperties = getAllApprovedProperties()
        return allProperties.filter { property ->
            try {
                val distance = calculateDistance(latitude, longitude, property.latitude, property.longitude)
                distance <= radiusKm
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371.0
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

    // ========== PROPERTY MANAGEMENT WITH NOTIFICATIONS ==========

    override fun addProperty(
        property: PropertyModel,
        callback: (Boolean, String?) -> Unit
    ) {
        val propertyId = ref.push().key

        if (propertyId == null) {
            callback(false, "Failed to generate ID")
            return
        }

        val propertyWithId = property.copy(id = propertyId.hashCode())

        ref.child(propertyId)
            .setValue(propertyWithId)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Property added successfully: ${property.title}")
                callback(true, null)

                // 🔔 NOTIFICATION: Notify all users about new property
                if (property.status == PropertyStatus.APPROVED) {
                    notifyAllUsersAboutNewProperty(propertyWithId)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ Failed to add property: ${it.message}")
                callback(false, it.message)
            }
    }

    // ========== NOTIFICATION FUNCTIONS ==========

    private fun notifyAllUsersAboutNewProperty(property: PropertyModel) {
        val currentUserId = auth.currentUser?.uid ?: property.ownerId

        usersRef.get().addOnSuccessListener { snapshot ->
            val allUserIds = snapshot.children.mapNotNull { it.key }
            val userIdsToNotify = allUserIds.filter { it != currentUserId }

            Log.d(TAG, "📢 Notifying ${userIdsToNotify.size} users about new property: ${property.title}")

            val imageUrl = if (property.images.isNotEmpty()) property.images[0] else ""

            val notificationData = hashMapOf<String, Any>(
                "title" to "🏠 New Property Listed!",
                "message" to "${property.title} is now available for ${property.marketType} in ${property.location}",
                "type" to "property",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "imageUrl" to imageUrl,
                "actionData" to property.id.toString()
            )

            userIdsToNotify.forEach { userId ->
                notificationsRef.child(userId).push().setValue(notificationData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Notification sent to user: $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to send notification to $userId: ${e.message}")
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to fetch users for notification: ${e.message}")
        }
    }

    fun notifyOwnerPropertyApproved(
        ownerId: String,
        propertyTitle: String,
        propertyId: Int,
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        val notificationData = hashMapOf<String, Any>(
            "title" to "✅ Property Approved!",
            "message" to "Congratulations! Your property '$propertyTitle' has been approved and is now live!",
            "type" to "system",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "imageUrl" to "",
            "actionData" to propertyId.toString()
        )

        notificationsRef.child(ownerId).push().setValue(notificationData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Approval notification sent to owner: $ownerId")
                callback?.invoke(true, "Notification sent")

                ref.orderByChild("id").equalTo(propertyId.toDouble())
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val property = snapshot.children.first()
                                    .getValue(PropertyModel::class.java)
                                property?.let { notifyAllUsersAboutNewProperty(it) }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Failed to fetch property for notification: ${error.message}")
                        }
                    })
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to send approval notification: ${e.message}")
                callback?.invoke(false, e.message ?: "Failed")
            }
    }

    fun notifyOwnerPropertyRejected(
        ownerId: String,
        propertyTitle: String,
        reason: String = "Does not meet listing standards",
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        val notificationData = hashMapOf<String, Any>(
            "title" to "❌ Property Listing Rejected",
            "message" to "Your property '$propertyTitle' was not approved. Reason: $reason. Please review and resubmit.",
            "type" to "system",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "imageUrl" to "",
            "actionData" to ""
        )

        notificationsRef.child(ownerId).push().setValue(notificationData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Rejection notification sent to owner: $ownerId")
                callback?.invoke(true, "Notification sent")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to send rejection notification: ${e.message}")
                callback?.invoke(false, e.message ?: "Failed")
            }
    }

    // ========== IMAGE UPLOAD ==========

    override fun uploadImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
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

    override fun uploadMultipleImages(context: Context, imageUris: List<Uri>, callback: (List<String>) -> Unit) {
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

    override fun getFileNameFromUri(context: Context, imageUri: Uri): String? {
        val cursor = context.contentResolver.query(imageUri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                return it.getString(nameIndex)
            }
        }
        return "image_${System.currentTimeMillis()}"
    }
}