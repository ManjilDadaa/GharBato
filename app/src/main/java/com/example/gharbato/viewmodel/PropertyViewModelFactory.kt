package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.repository.SavedPropertiesRepository

class PropertyViewModelFactory(
    private val repository: PropertyRepo,
    private val savedPropertiesRepository: SavedPropertiesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            return PropertyViewModel(repository, savedPropertiesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}