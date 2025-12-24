package com.example.gharbato.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.repository.SavedPropertiesRepository

class SavedPropertiesViewModelFactory(
    private val repository: SavedPropertiesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedPropertiesViewModel::class.java)) {
            return SavedPropertiesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}