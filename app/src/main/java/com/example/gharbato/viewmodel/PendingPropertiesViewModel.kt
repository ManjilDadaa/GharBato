package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.PendingPropertiesRepo
import com.example.gharbato.repository.PendingPropertiesRepoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PendingPropertiesVM"

data class PendingPropertiesUiState(
    val properties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class PendingPropertiesViewModel(
    private val repository: PendingPropertiesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingPropertiesUiState())
    val uiState: StateFlow<PendingPropertiesUiState> = _uiState.asStateFlow()

    init {
        loadPendingProperties()
    }

    private fun loadPendingProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getPendingProperties().collect { properties ->
                    _uiState.value = _uiState.value.copy(
                        properties = properties,
                        isLoading = false,
                        error = null
                    )
                    Log.d(TAG, "Loaded ${properties.size} pending properties")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pending properties: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load pending properties"
                )
            }
        }
    }

    fun approveProperty(propertyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.approveProperty(propertyId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Property approved successfully"
                    )
                    Log.d(TAG, "Property $propertyId approved")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to approve property"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error approving property: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to approve property"
                )
            }
        }
    }

    fun rejectProperty(propertyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.rejectProperty(propertyId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Property rejected"
                    )
                    Log.d(TAG, "Property $propertyId rejected")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to reject property"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting property: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reject property"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null
        )
    }
}

class PendingPropertiesViewModelFactory(
    private val repository: PendingPropertiesRepo = PendingPropertiesRepoImpl()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PendingPropertiesViewModel::class.java)) {
            return PendingPropertiesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}