// PendingListingsViewModel.kt
package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.repository.PendingPropertiesRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PendingPropertiesViewModel(
    private val repository: PendingPropertiesRepo
) : ViewModel() {

    private val _pendingProperties = MutableStateFlow<List<PropertyModel>>(emptyList())
    val pendingProperties: StateFlow<List<PropertyModel>> = _pendingProperties

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchPendingProperties() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.getPendingProperties()
            _pendingProperties.value = list
            _isLoading.value = false
        }
    }

    fun approveProperty(propertyId: Int) {
        viewModelScope.launch {
            repository.approveProperty(propertyId)
            fetchPendingProperties() // Refresh list
        }
    }

    fun rejectProperty(propertyId: Int) {
        viewModelScope.launch {
            repository.rejectProperty(propertyId)
            fetchPendingProperties() // Refresh list
        }
    }
}
