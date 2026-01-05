package com.example.gharbato.data.repository

import com.example.gharbato.repository.SavedPropertiesRepository
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl

object RepositoryProvider {
    // Singleton instances - all screens will share these
    private val savedPropertiesRepository: SavedPropertiesRepositoryImpl by lazy {
        SavedPropertiesRepositoryImpl()
    }

    private val propertyRepository: PropertyRepoImpl by lazy {
        PropertyRepoImpl()
    }

    fun getSavedPropertiesRepository(): SavedPropertiesRepository {
        return savedPropertiesRepository
    }

    fun getPropertyRepository(): PropertyRepo {
        return propertyRepository
    }
}