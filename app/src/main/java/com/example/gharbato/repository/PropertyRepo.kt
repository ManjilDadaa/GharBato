package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.model.PropertyFilters

interface PropertyRepo {

    // Property Retrieval
    suspend fun getAllProperties(): List<PropertyModel> // For admins only
    suspend fun getAllApprovedProperties(): List<PropertyModel>
    suspend fun getPropertyById(id: Int): PropertyModel?



    // Search & Filter
    suspend fun searchProperties(query: String): List<PropertyModel>
    suspend fun filterProperties(filters: PropertyFilters): List<PropertyModel>

    // Location-based Search
    suspend fun getPropertiesByLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel>

    // Property Management
    fun addProperty(
        property: PropertyModel,
        callback: (Boolean, String?) -> Unit
    )

    // Image Upload
    fun uploadImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    )

    fun uploadMultipleImages(
        context: Context,
        imageUris: List<Uri>,
        callback: (List<String>) -> Unit
    )

    fun getFileNameFromUri(
        context: Context,
        imageUri: Uri
    ): String?
}