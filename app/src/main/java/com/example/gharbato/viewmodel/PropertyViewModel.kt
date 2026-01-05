package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PropertyViewModel"

// ========== UI STATE ==========

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val selectedProperty: PropertyModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "Buy",
    val selectedPropertyType: String = "All", // ✅ CHANGED to "All" by default
    val minPrice: Int = 0, // ✅ CHANGED to 0 (no minimum)
    val showMap: Boolean = true,
    val searchLocation: SearchLocation? = null
)

data class SearchLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
)

// ========== VIEW MODEL ==========

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

    // ========== PROPERTY LOADING ==========

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Loading all properties...")
                val properties = repository.getAllProperties()
                Log.d(TAG, "Loaded ${properties.size} properties from repository")

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
                Log.d(TAG, "UI updated with ${propertiesWithFavoriteStatus.size} properties")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading properties", e)
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
        // ✅ Auto-search as user types (optional - remove if you want manual search only)
        if (query.length >= 2 || query.isEmpty()) {
            performSearch()
        }
    }

    /**
     * Perform text-based search
     */
    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        Log.d(TAG, "Performing search with query: '$query'")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val properties = repository.searchProperties(query)
                Log.d(TAG, "Search returned ${properties.size} properties")

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // ✅ FIXED: Only apply filters if they're actually set
                val filteredProperties = if (shouldApplyFilters()) {
                    Log.d(TAG, "Applying filters...")
                    applyCurrentFilters(propertiesWithFavoriteStatus)
                } else {
                    Log.d(TAG, "No filters applied")
                    propertiesWithFavoriteStatus
                }

                Log.d(TAG, "Final result: ${filteredProperties.size} properties")

                _uiState.value = _uiState.value.copy(
                    properties = filteredProperties,
                    isLoading = false,
                    error = if (filteredProperties.isEmpty()) "No properties found" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during search", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Search properties by location and radius
     */
    fun searchByLocation(
        latitude: Double,
        longitude: Double,
        address: String,
        radiusKm: Float
    ) {
        Log.d(TAG, "Searching by location: $address, radius: ${radiusKm}km")

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

                Log.d(TAG, "Location search returned ${properties.size} properties")

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // ✅ FIXED: Only apply filters if they're actually set
                val filteredProperties = if (shouldApplyFilters()) {
                    applyCurrentFilters(propertiesWithFavoriteStatus)
                } else {
                    propertiesWithFavoriteStatus
                }

                _uiState.value = _uiState.value.copy(
                    properties = filteredProperties,
                    isLoading = false,
                    error = if (filteredProperties.isEmpty())
                        "No properties found in this area" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during location search", e)
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
        // Re-apply search with new filter
        if (_uiState.value.searchQuery.isNotEmpty() || _uiState.value.searchLocation != null) {
            performSearch()
        } else {
            loadProperties()
        }
    }

    fun updatePropertyType(type: String) {
        _uiState.value = _uiState.value.copy(selectedPropertyType = type)
        // Re-apply search with new filter
        if (_uiState.value.searchQuery.isNotEmpty() || _uiState.value.searchLocation != null) {
            performSearch()
        } else {
            loadProperties()
        }
    }

    fun updateMinPrice(price: Int) {
        _uiState.value = _uiState.value.copy(minPrice = price)
        // Re-apply search with new filter
        if (_uiState.value.searchQuery.isNotEmpty() || _uiState.value.searchLocation != null) {
            performSearch()
        } else {
            loadProperties()
        }
    }

    /**
     * Check if filters should be applied
     */
    private fun shouldApplyFilters(): Boolean {
        val state = _uiState.value
        return state.selectedPropertyType != "All" || state.minPrice > 0
    }

    /**
     * Apply current filter state to a list of properties
     */
    private fun applyCurrentFilters(properties: List<PropertyModel>): List<PropertyModel> {
        var filtered = properties
        val state = _uiState.value

        Log.d(TAG, "Applying filters - Market: ${state.selectedMarketType}, Type: ${state.selectedPropertyType}, MinPrice: ${state.minPrice}")

        // FIXED: Filter by property type (only if not "All")
        if (state.selectedPropertyType != "All" && state.selectedPropertyType.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.propertyType.equals(state.selectedPropertyType, ignoreCase = true)
            }
            Log.d(TAG, "After property type filter: ${filtered.size} properties")
        }

        //FIXED: Filter by minimum price (only if > 0)
        if (state.minPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                priceValue >= (state.minPrice * 1000)
            }
            Log.d(TAG, "After price filter: ${filtered.size} properties")
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

    // ========== FAVORITES ==========

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

    // ========== UI STATE ==========

    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }

    // ========== IMAGE UPLOAD ==========

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    ) {
        repository.uploadImage(context, imageUri, callback)
    }
}