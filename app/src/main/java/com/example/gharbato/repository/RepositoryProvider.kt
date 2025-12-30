package com.example.gharbato.data.repository

import com.example.gharbato.repository.SavedPropertiesRepository
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl

object RepositoryProvider {
    // Singleton instances - all screens will share these
    private val savedPropertiesRepository: SavedPropertiesRepositoryImpl by lazy {
        SavedPropertiesRepositoryImpl()
    }

    private val propertyRepository: PropertyRepositoryImpl by lazy {
        PropertyRepositoryImpl()
    }

    fun getSavedPropertiesRepository(): SavedPropertiesRepository {
        return savedPropertiesRepository
    }

    fun getPropertyRepository(): PropertyRepository {
        return propertyRepository
    }
}