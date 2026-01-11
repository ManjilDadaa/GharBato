package com.example.gharbato.repository

import com.example.gharbato.model.PropertyModel
import kotlinx.coroutines.flow.Flow

interface SavedPropertiesRepository {
    suspend fun getSavedProperties(): List<PropertyModel>
    suspend fun saveProperty(property: PropertyModel)
    suspend fun removeSavedProperty(propertyId: Int)
    suspend fun removeProperty(propertyId: Int) // Add this alias for consistency
    suspend fun isPropertySaved(propertyId: Int): Boolean
    fun getSavedPropertiesFlow(): Flow<List<PropertyModel>>
}