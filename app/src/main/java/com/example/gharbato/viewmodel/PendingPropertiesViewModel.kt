// PendingListingsViewModel.kt
package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.repository.PendingPropertiesRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class PendingPropertiesViewModel(
    private val repository: PendingPropertiesRepo
) : ViewModel() {

    private val _pendingProperties = MutableStateFlow<List<PropertyModel>>(emptyList())
    val pendingProperties: StateFlow<List<PropertyModel>> = _pendingProperties

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchPendingProperties()
    }

     fun fetchPendingProperties() {
        viewModelScope.launch {
            repository.getPendingProperties()
                .onStart { _isLoading.value = true }
                .catch { e ->
                    // Handle error
                    _isLoading.value = false
                }
                .collect { properties ->
                    _pendingProperties.value = properties
                    _isLoading.value = false
                }
        }
    }

    fun approveProperty(propertyId: Int) {
        viewModelScope.launch {
            repository.approveProperty(propertyId)
            // No need to call fetchPendingProperties(), flow will update automatically
        }
    }

    fun rejectProperty(propertyId: Int) {
        viewModelScope.launch {
            repository.rejectProperty(propertyId)
            // No need to call fetchPendingProperties(), flow will update automatically
        }
    }
}