package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val selectedProperty: PropertyModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "Buy",
    val selectedPropertyType: String = "Secondary market",
    val minPrice: Int = 16,
    val showMap: Boolean = true
)

class PropertyViewModel(
    private val repository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    init {
        loadProperties()
    }

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val properties = repository.getAllProperties()
                _uiState.value = _uiState.value.copy(
                    properties = properties,
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

    fun selectProperty(property: PropertyModel) {
        _uiState.value = _uiState.value.copy(selectedProperty = property)
    }

    fun clearSelectedProperty() {
        _uiState.value = _uiState.value.copy(selectedProperty = null)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchProperties(query)
    }

    fun updateMarketType(type: String) {
        _uiState.value = _uiState.value.copy(selectedMarketType = type)
        filterProperties()
    }

    fun updatePropertyType(type: String) {
        _uiState.value = _uiState.value.copy(selectedPropertyType = type)
        filterProperties()
    }

    fun updateMinPrice(price: Int) {
        _uiState.value = _uiState.value.copy(minPrice = price)
        filterProperties()
    }

    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }

    private fun searchProperties(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val properties = repository.searchProperties(query)
                _uiState.value = _uiState.value.copy(
                    properties = properties,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun filterProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val properties = repository.filterProperties(
                    marketType = _uiState.value.selectedMarketType,
                    propertyType = _uiState.value.selectedPropertyType,
                    minPrice = _uiState.value.minPrice,
                    bedrooms = 9
                )
                _uiState.value = _uiState.value.copy(
                    properties = properties,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun getPropertyById(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val property = repository.getPropertyById(id)
                _uiState.value = _uiState.value.copy(
                    selectedProperty = property,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}