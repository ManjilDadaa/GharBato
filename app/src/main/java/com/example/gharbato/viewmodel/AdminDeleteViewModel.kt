package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.AdminDeleteRepo
import com.example.gharbato.repository.AdminDeleteRepoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AdminDeleteVM"

data class AdminDeleteUiState(
    val rejectedProperties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AdminDeleteViewModel(
    private val repository: AdminDeleteRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDeleteUiState())
    val uiState: StateFlow<AdminDeleteUiState> = _uiState.asStateFlow()

    init {
        loadRejectedProperties()
    }

    private fun loadRejectedProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getRejectedProperties().collect { properties ->
                    _uiState.value = _uiState.value.copy(
                        rejectedProperties = properties,
                        isLoading = false,
                        error = null
                    )
                    Log.d(TAG, "Loaded ${properties.size} rejected properties")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rejected properties: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load rejected properties"
                )
            }
        }
    }

    fun deleteProperty(propertyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.deleteProperty(propertyId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Property deleted permanently"
                    )
                    Log.d(TAG, "Property $propertyId deleted")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete property"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting property: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete property"
                )
            }
        }
    }

    fun restoreProperty(propertyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.restoreProperty(propertyId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Property restored to pending status"
                    )
                    Log.d(TAG, "Property $propertyId restored")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to restore property"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring property: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to restore property"
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

class AdminDeleteViewModelFactory(
    private val repository: AdminDeleteRepo = AdminDeleteRepoImpl()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDeleteViewModel::class.java)) {
            return AdminDeleteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}