package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

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

    private var observationJob: Job? = null

    fun retry() {
        observeSavedProperties()
    }

    init {
        observeSavedProperties()
    }

    private fun observeSavedProperties() {
        observationJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        observationJob = viewModelScope.launch {
            repository.getSavedPropertiesFlow().catch {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Couldn't load saved properties. Please retry.")
            }.collect { properties ->
                _uiState.value = _uiState.value.copy(
                    savedProperties = properties, isLoading = false, error = null
                )
            }
        }
    }

    fun removeFromSaved(propertyId: Int) {
        viewModelScope.launch {
            try {
                repository.removeSavedProperty(propertyId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(error = "Couldn't update saved properties. Please retry.")
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
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(error = "Couldn't update saved properties. Please retry.")
            }
        }
    }
}