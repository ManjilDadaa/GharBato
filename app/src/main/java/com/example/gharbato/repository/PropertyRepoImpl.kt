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
            title = "Modern Apartment",
            developer = "Ram Sharma",
            price = "from 8.7 lakh",
            sqft = "1200 sq.ft",
            bedrooms = 2,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800",
            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "Apartment",
            floor = "5th Floor",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 2,
            title = "Luxury Villa",
            developer = "Shyam Builders",
            price = "from 15.8 lakh",
            sqft = "2500 sq.ft",
            bedrooms = 3,
            bathrooms = 3,
            imageUrl = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800",
            location = "Lalitpur",
            latLng = LatLng(27.6710, 85.3240),
            propertyType = "Villa",
            floor = "Ground Floor",
            furnishing = "Semi Furnished",
            parking = true,
            petsAllowed = true
        ),
        PropertyModel(
            id = 3,
            title = "Cozy House",
            developer = "Krishna Estates",
            price = "from 12.6 lakh",
            sqft = "1800 sq.ft",
            bedrooms = 3,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800",
            location = "Bhaktapur",
            latLng = LatLng(27.6710, 85.4298),
            propertyType = "House",
            floor = "2 Storey",
            furnishing = "Unfurnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 4,
            title = "Penthouse Suite",
            developer = "Hari Properties",
            price = "13.2 lakh",
            sqft = "2000 sq.ft",
            bedrooms = 2,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
            location = "Pokhara",
            latLng = LatLng(28.2096, 83.9856),
            propertyType = "Penthouse",
            floor = "Top Floor",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 5,
            title = "Family Home",
            developer = "Sita Developers",
            price = "from 10.5 lakh",
            sqft = "1500 sq.ft",
            bedrooms = 3,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800",
            location = "Kathmandu",
            latLng = LatLng(27.7000, 85.3200),
            propertyType = "House",
            floor = "1 Storey",
            furnishing = "Semi Furnished",
            parking = false,
            petsAllowed = true
        )
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