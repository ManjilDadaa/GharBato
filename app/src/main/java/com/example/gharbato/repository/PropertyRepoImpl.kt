package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.data.model.PropertyModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import java.io.InputStream
import java.util.concurrent.Executors

class PropertyRepoImpl : PropertyRepo {

    // Sample data - In real app, this would be from API/Firebase
    private val sampleProperties = listOf(
        PropertyModel(
            id = 1,
            title = "Luxury Villa",
            developer = "ABC Builders",
            price = "Rs 2,50,00,000",
            sqft = "2500 sq.ft",
            bedrooms = 4,
            bathrooms = 3,

            images = mapOf(
                "cover" to listOf(
                    "https://picsum.photos/600/400"
                ),
                "bedrooms" to listOf(
                    "https://picsum.photos/600/401",
                    "https://picsum.photos/600/402"
                ),
                "kitchen" to listOf(
                    "https://picsum.photos/600/403"
                )
            ),

            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "House",
            floor = "2",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = true
        ),
        PropertyModel(
            id = 1,
            title = "Luxury Villa",
            developer = "ABC Builders",
            price = "Rs 2,50,00,000",
            sqft = "2500 sq.ft",
            bedrooms = 4,
            bathrooms = 3,

            images = mapOf(
                "cover" to listOf(
                    "https://picsum.photos/600/400"
                ),
                "bedrooms" to listOf(
                    "https://picsum.photos/600/401",
                    "https://picsum.photos/600/402"
                ),
                "kitchen" to listOf(
                    "https://picsum.photos/600/403"
                )
            ),

            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "House",
            floor = "2",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = true
        ),

    )

    private val _properties = mutableListOf<PropertyModel>()

    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val ref: DatabaseReference = database.getReference("Property")
    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    override suspend fun getAllProperties(): List<PropertyModel> {
        // Simulate network delay
        delay(500)
        return sampleProperties
    }

    override suspend fun getPropertyById(id: Int): PropertyModel? {
        delay(300)
        return sampleProperties.find { it.id == id }
    }

    override suspend fun searchProperties(query: String): List<PropertyModel> {
        delay(400)
        return if (query.isEmpty()) {
            sampleProperties
        } else {
            sampleProperties.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true) ||
                        it.developer.contains(query, ignoreCase = true)
            }
        }
    }

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
                                "folder", "properties" // Organize in folder
                            )
                        )

                        var imageUrl = response["url"] as String?
                        imageUrl = imageUrl?.replace("http://", "https://")

                        if (imageUrl != null) {
                            uploadedUrls.add(imageUrl)
                            println("Image uploaded: $imageUrl")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other images even if one fails
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

    override suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel> {
        delay(400)
        return sampleProperties.filter { property ->
            // Add your filtering logic here
            true // For now, return all
        }
    }
}