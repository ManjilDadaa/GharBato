package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.data.repository.PropertyRepoImpl
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AdminSearchVM"

data class AdminSearchUiState(
    val allProperties: List<PropertyModel> = emptyList(),
    val pendingProperties: List<PropertyModel> = emptyList(),
    val approvedProperties: List<PropertyModel> = emptyList(),
    val rejectedProperties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminSearchViewModel(
    private val repository: PropertyRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminSearchUiState())
    val uiState: StateFlow<AdminSearchUiState> = _uiState.asStateFlow()

    fun searchProperties(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d(TAG, "Searching properties with query: $query")

                // Get all properties (not just approved)
                val allProperties = repository.getAllProperties()

                // Filter by search query
                val searchQuery = query.lowercase().trim()
                val filteredProperties = allProperties.filter { property ->
                    property.title.lowercase().contains(searchQuery) ||
                            property.location.lowercase().contains(searchQuery) ||
                            property.ownerName.lowercase().contains(searchQuery) ||
                            property.ownerEmail.lowercase().contains(searchQuery) ||
                            property.developer.lowercase().contains(searchQuery) ||
                            property.propertyType.lowercase().contains(searchQuery)
                }

                Log.d(TAG, "Found ${filteredProperties.size} matching properties")

                // Categorize by status
                val pending = filteredProperties.filter { it.status == PropertyStatus.PENDING }
                val approved = filteredProperties.filter { it.status == PropertyStatus.APPROVED }
                val rejected = filteredProperties.filter { it.status == PropertyStatus.REJECTED }

                _uiState.value = _uiState.value.copy(
                    allProperties = filteredProperties,
                    pendingProperties = pending,
                    approvedProperties = approved,
                    rejectedProperties = rejected,
                    isLoading = false,
                    error = null
                )

                Log.d(TAG, "Results - All: ${filteredProperties.size}, " +
                        "Pending: ${pending.size}, " +
                        "Approved: ${approved.size}, " +
                        "Rejected: ${rejected.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error searching properties: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to search properties"
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = AdminSearchUiState()
    }
}

class AdminSearchViewModelFactory(
    private val repository: PropertyRepo = PropertyRepoImpl()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminSearchViewModel::class.java)) {
            return AdminSearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}