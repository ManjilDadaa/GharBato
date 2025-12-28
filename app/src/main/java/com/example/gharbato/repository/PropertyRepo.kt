package com.example.gharbato.data.repository

import android.content.Context
import android.net.Uri
import com.example.gharbato.data.model.PropertyModel

interface PropertyRepository {
    suspend fun getAllProperties(): List<PropertyModel>
    suspend fun getPropertyById(id: Int): PropertyModel?
    suspend fun searchProperties(query: String): List<PropertyModel>

    fun uploadImage(context: Context,
                    imageUri: Uri,
                    callback: (String?) -> Unit
    ) // android.net bata

    fun getFileNameFromUri(context: Context, imageUri: Uri) : String?
    suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel>
}