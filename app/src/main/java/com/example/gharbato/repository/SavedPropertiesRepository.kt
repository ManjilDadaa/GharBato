package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import kotlinx.coroutines.flow.Flow

interface SavedPropertiesRepository {
    suspend fun getSavedProperties(): List<PropertyModel>
    suspend fun saveProperty(property: PropertyModel)
    suspend fun removeSavedProperty(propertyId: Int)
    suspend fun isPropertySaved(propertyId: Int): Boolean
    fun getSavedPropertiesFlow(): Flow<List<PropertyModel>>
}