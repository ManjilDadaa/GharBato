package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.data.model.PropertyModel
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    // ✅ STEP 1: Get All Properties from Firebase
    override suspend fun getAllProperties(): List<PropertyModel> {
        return suspendCancellableCoroutine { continuation ->
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val properties = mutableListOf<PropertyModel>()

                    for (propertySnapshot in snapshot.children) {
                        try {
                            val property = propertySnapshot.getValue(PropertyModel::class.java)
                            property?.let { properties.add(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Skip invalid properties
                        }
                    }

                    continuation.resume(properties)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(
                        Exception("Firebase error: ${error.message}")
                    )
                }
            })
        }
    }

    // ✅ STEP 2: Get Property by ID from Firebase
    override suspend fun getPropertyById(id: Int): PropertyModel? {
        return suspendCancellableCoroutine { continuation ->
            ref.orderByChild("id")
                .equalTo(id.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val property = snapshot.children.firstOrNull()
                            ?.getValue(PropertyModel::class.java)
                        continuation.resume(property)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(
                            Exception("Firebase error: ${error.message}")
                        )
                    }
                })
        }
    }

    // ✅ STEP 3: Search Properties
    override suspend fun searchProperties(query: String): List<PropertyModel> {
        val allProperties = getAllProperties()

        return if (query.isEmpty()) {
            allProperties
        } else {
            allProperties.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true) ||
                        it.developer.contains(query, ignoreCase = true) ||
                        it.propertyType.contains(query, ignoreCase = true)
            }
        }
    }

    // ✅ STEP 4: Filter Properties
    override suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel> {
        val allProperties = getAllProperties()

        return allProperties.filter { property ->
            // Add your filtering logic here
            // For now, return all properties
            // You can add filters based on marketType, propertyType, etc.
            true
        }
    }

    // ✅ Add Property to Firebase (already working)
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
                callback(true, null)
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    // ✅ Image Upload (already working)
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

    // ✅ Upload Multiple Images (already working)
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