package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.data.repository.PropertyRepository

class PropertyViewModelFactory(
    private val repository: PropertyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            return PropertyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}