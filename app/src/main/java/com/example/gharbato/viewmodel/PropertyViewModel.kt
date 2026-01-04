package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
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
    val showMap: Boolean = true,
    val errorMessage: String = "",
    val searchLocation: SearchLocation? = null
)

data class SearchLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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

    fun getPropertyById(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val property = repository.getPropertyById(id)
                val propertyWithFavoriteStatus = property?.copy(
                    isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                )
                _uiState.value = _uiState.value.copy(
                    selectedProperty = propertyWithFavoriteStatus,
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

    // ========== SEARCH FUNCTIONS ==========

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * Perform text-based search
     */
    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        searchProperties(query)
    }

    private fun searchProperties(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val properties = repository.searchProperties(query)
                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // Apply current filters
                val filteredProperties = applyCurrentFilters(propertiesWithFavoriteStatus)

                _uiState.value = _uiState.value.copy(
                    properties = filteredProperties,
                    isLoading = false,
                    error = if (filteredProperties.isEmpty()) "No properties found" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * NEW: Search properties by location and radius
     */
    fun searchByLocation(
        latitude: Double,
        longitude: Double,
        address: String,
        radiusKm: Float
    ) {
        _uiState.value = _uiState.value.copy(
            searchLocation = SearchLocation(latitude, longitude, address, radiusKm),
            searchQuery = address
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val properties = repository.getPropertiesByLocation(
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = radiusKm
                )

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // Apply current filters
                val filteredProperties = applyCurrentFilters(propertiesWithFavoriteStatus)

                _uiState.value = _uiState.value.copy(
                    properties = filteredProperties,
                    isLoading = false,
                    error = if (filteredProperties.isEmpty())
                        "No properties found in this area" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Clear search and reset to all properties
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchLocation = null
        )
        loadProperties()
    }

    // ========== FILTER FUNCTIONS ==========

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

    private fun filterProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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

    /**
     * Apply current filter state to a list of properties
     */
    private fun applyCurrentFilters(properties: List<PropertyModel>): List<PropertyModel> {
        var filtered = properties

        // Filter by market type (Buy/Rent)
        filtered = filtered.filter { property ->
            property.marketType.equals(_uiState.value.selectedMarketType, ignoreCase = true)
        }

        // Filter by property type
        if (_uiState.value.selectedPropertyType != "All") {
            filtered = filtered.filter { property ->
                property.propertyType.equals(_uiState.value.selectedPropertyType, ignoreCase = true)
            }
        }

        // Filter by minimum price
        filtered = filtered.filter { property ->
            val priceValue = extractPriceValue(property.price)
            priceValue >= (_uiState.value.minPrice * 1000)
        }

        return filtered
    }

    /**
     * Extract numeric price from price string
     */
    private fun extractPriceValue(priceString: String): Int {
        val numbers = priceString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }

    // ========== PROPERTY SELECTION ==========

    fun selectProperty(property: PropertyModel) {
        _uiState.value = _uiState.value.copy(selectedProperty = property)
    }

    fun clearSelectedProperty() {
        _uiState.value = _uiState.value.copy(selectedProperty = null)
    }


    fun toggleFavorite(property: PropertyModel) {
        viewModelScope.launch {
            try {
                if (property.isFavorite) {
                    savedPropertiesRepository.removeSavedProperty(property.id)
                } else {
                    savedPropertiesRepository.saveProperty(property)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }


    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }


    fun uploadImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    ) {
        repository.uploadImage(context, imageUri, callback)
    }
}