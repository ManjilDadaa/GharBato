package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedPropertiesRepositoryImpl : SavedPropertiesRepository {

    // In-memory storage (in real app, use Room database or SharedPreferences)
    private val savedPropertiesMap = mutableMapOf<Int, PropertyModel>()
    private val _savedPropertiesFlow = MutableStateFlow<List<PropertyModel>>(emptyList())

    override suspend fun getSavedProperties(): List<PropertyModel> {
        return savedPropertiesMap.values.toList()
    }

    override suspend fun saveProperty(property: PropertyModel) {
        savedPropertiesMap[property.id] = property.copy(isFavorite = true)
        updateFlow()
    }

    override suspend fun removeSavedProperty(propertyId: Int) {
        savedPropertiesMap.remove(propertyId)
        updateFlow()
    }

    // Alias method for consistency
    override suspend fun removeProperty(propertyId: Int) {
        removeSavedProperty(propertyId)
    }

    override suspend fun isPropertySaved(propertyId: Int): Boolean {
        return savedPropertiesMap.containsKey(propertyId)
    }

    override fun getSavedPropertiesFlow(): Flow<List<PropertyModel>> {
        return _savedPropertiesFlow.asStateFlow()
    }

    private fun updateFlow() {
        _savedPropertiesFlow.value = savedPropertiesMap.values.toList()
    }
}