package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import com.example.gharbato.data.model.PropertyModel

interface PropertyRepo {
    suspend fun getAllProperties(): List<PropertyModel>

    suspend fun getPropertyById(id: Int): PropertyModel?

    suspend fun searchProperties(query: String): List<PropertyModel>

    suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel>

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    )

    fun addProperty(
        property: PropertyModel,
        callback: (Boolean, String?) -> Unit
    )

    fun uploadMultipleImages(
        context: Context,
        imageUris: List<Uri>,
        callback: (List<String>) -> Unit
    )

    fun getFileNameFromUri(context: Context, imageUri: Uri): String?

    suspend fun getPropertiesByLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel>
}