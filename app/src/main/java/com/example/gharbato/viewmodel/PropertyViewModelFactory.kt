package com.example.gharbato.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.data.repository.PropertyRepoImpl
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl
import com.example.gharbato.repository.SearchFilterRepoImpl


class PropertyViewModelFactory(
    private val context: Context  
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PropertyViewModel::class.java) -> {
                PropertyViewModel(
                    repository = PropertyRepoImpl(),
                    savedPropertiesRepository = SavedPropertiesRepositoryImpl(),
                    searchFilterRepo = SearchFilterRepoImpl()
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}