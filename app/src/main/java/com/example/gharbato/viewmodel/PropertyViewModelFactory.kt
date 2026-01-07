package com.example.gharbato.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.data.repository.PropertyRepoImpl
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl
import com.example.gharbato.repository.SearchFilterRepoImpl

class PropertyViewModelFactory(context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            val propertyRepo = PropertyRepoImpl()
            val savedPropertiesRepo = SavedPropertiesRepositoryImpl()
            val searchFilterRepo = SearchFilterRepoImpl()

            return PropertyViewModel(
                propertyRepo,
                savedPropertiesRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}