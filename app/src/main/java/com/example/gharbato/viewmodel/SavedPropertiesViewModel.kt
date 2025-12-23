package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SavedPropertiesUiState(
    val savedProperties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SavedPropertiesViewModel(
    private val repository: SavedPropertiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedPropertiesUiState())
    val uiState: StateFlow<SavedPropertiesUiState> = _uiState.asStateFlow()

    init {
        loadSavedProperties()
        observeSavedProperties()
    }

    private fun loadSavedProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val properties = repository.getSavedProperties()
                _uiState.value = _uiState.value.copy(
                    savedProperties = properties,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun observeSavedProperties() {
        viewModelScope.launch {
            repository.getSavedPropertiesFlow().collect { properties ->
                _uiState.value = _uiState.value.copy(
                    savedProperties = properties
                )
            }
        }
    }

    fun removeFromSaved(propertyId: Int) {
        viewModelScope.launch {
            try {
                repository.removeSavedProperty(propertyId)
                loadSavedProperties()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleFavorite(property: PropertyModel) {
        viewModelScope.launch {
            try {
                if (property.isFavorite) {
                    repository.removeSavedProperty(property.id)
                } else {
                    repository.saveProperty(property)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}