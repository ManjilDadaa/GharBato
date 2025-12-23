package com.example.gharbato.data.repository

import com.example.gharbato.data.model.PropertyModel

interface PropertyRepository {
    suspend fun getAllProperties(): List<PropertyModel>
    suspend fun getPropertyById(id: Int): PropertyModel?
    suspend fun searchProperties(query: String): List<PropertyModel>
    suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel>
}