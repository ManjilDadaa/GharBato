package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.repository.SavedPropertiesRepository
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
    private val repository: PropertyRepo,
    private val savedPropertiesRepository: SavedPropertiesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyUiState())
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    init {
        loadProperties()
        observeSavedProperties()
    }

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val properties = repository.getAllProperties()
                // Check which properties are saved
                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    properties = propertiesWithFavoriteStatus,
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
            savedPropertiesRepository.getSavedPropertiesFlow().collect { savedProperties ->
                val savedIds = savedProperties.map { it.id }.toSet()

                val updatedProperties = _uiState.value.properties.map { property ->
                    property.copy(isFavorite = savedIds.contains(property.id))
                }


                val updatedSelectedProperty = _uiState.value.selectedProperty?.let { selected ->
                    selected.copy(isFavorite = savedIds.contains(selected.id))
                }

                _uiState.value = _uiState.value.copy(
                    properties = updatedProperties,
                    selectedProperty = updatedSelectedProperty
                )
            }
        }
    }

    fun toggleFavorite(property: PropertyModel) {
        viewModelScope.launch {
            try {
                if (property.isFavorite) {
                    savedPropertiesRepository.removeSavedProperty(property.id)
                } else {
                    savedPropertiesRepository.saveProperty(property)
                }
                // Refresh properties to update favorite status
                loadProperties()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
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
                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    properties = propertiesWithFavoriteStatus,
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
                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    properties = propertiesWithFavoriteStatus,
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
                val propertyWithFavoriteStatus = property?.copy(
                    isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                )
                _uiState.value = _uiState.value.copy(
                    selectedProperty = propertyWithFavoriteStatus,
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

    fun uploadImage(context: Context,
                    imageUri: Uri,
                    callback: (String?) -> Unit
    ){
        repository.uploadImage(context , imageUri, callback)
    }

//    fun submitListing(context: Context, state: PropertyListingState){
//
//    }

}