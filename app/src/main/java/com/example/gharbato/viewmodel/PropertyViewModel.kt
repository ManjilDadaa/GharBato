package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PropertyViewModel"

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val selectedProperty: PropertyModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "Buy",
    val selectedPropertyType: String = "All",
    val minPrice: Int = 0,
    val showMap: Boolean = true,
    val searchLocation: SearchLocation? = null,
    val currentFilters: PropertyFilters = PropertyFilters()
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
                val properties = repository.getAllApprovedProperties()
                Log.d(TAG, "Loaded ${properties.size} properties from repository")

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // Log property market types for debugging
                properties.forEach { property ->
                    Log.d(TAG, "Property: ${property.title} - MarketType: '${property.marketType}'")
                }

                // Don't apply filters on initial load - show all properties
                Log.d(TAG, "Initial load - showing all ${propertiesWithFavoriteStatus.size} properties")

                _uiState.value = _uiState.value.copy(
                    properties = propertiesWithFavoriteStatus,
                    isLoading = false,
                    error = if (propertiesWithFavoriteStatus.isEmpty()) "No properties found" else null
                )
                Log.d(TAG, "UI updated with ${propertiesWithFavoriteStatus.size} properties")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading properties", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load properties"
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

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        Log.d(TAG, "Performing search with query: '$query'")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val properties = if (query.isEmpty()) {
                    repository.getAllProperties()
                } else {
                    repository.searchProperties(query)
                }
                Log.d(TAG, "Search returned ${properties.size} properties")

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // Apply market type filter (always visible in UI) + any other active filters
                val filteredProperties = applyFiltersToList(propertiesWithFavoriteStatus)

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

                val filteredProperties = applyFiltersToList(propertiesWithFavoriteStatus)

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

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchLocation = null
        )
        loadProperties()
    }

    // ========== FILTER FUNCTIONS ==========

    fun getCurrentFilters(): PropertyFilters {
        return _uiState.value.currentFilters
    }

    /**
     * Check if user has set any filters
     */
    private fun hasActiveFilters(): Boolean {
        val filters = _uiState.value.currentFilters
        return filters.propertyTypes.isNotEmpty() ||
                filters.minPrice > 0 ||
                filters.maxPrice > 0 ||
                filters.bedrooms.isNotEmpty() ||
                filters.furnishing.isNotEmpty() ||
                filters.parking != null ||
                filters.petsAllowed != null ||
                filters.amenities.isNotEmpty() ||
                filters.floor.isNotEmpty()
    }

    fun applyFilters(filters: PropertyFilters) {
        Log.d(TAG, "Applying filters: $filters")

        _uiState.value = _uiState.value.copy(
            currentFilters = filters,
            selectedMarketType = filters.marketType,
            selectedPropertyType = if (filters.propertyTypes.isEmpty()) "All" else filters.propertyTypes.first(),
            minPrice = filters.minPrice
        )

        // Re-run current search/load with new filters
        if (_uiState.value.searchLocation != null) {
            val location = _uiState.value.searchLocation!!
            searchByLocation(location.latitude, location.longitude, location.address, location.radiusKm)
        } else if (_uiState.value.searchQuery.isNotEmpty()) {
            performSearch()
        } else {
            loadProperties()
        }
    }

    fun updateMarketType(type: String) {
        Log.d(TAG, "Market type changed to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(marketType = type)
        _uiState.value = _uiState.value.copy(
            currentFilters = updatedFilters,
            selectedMarketType = type
        )
        // Re-apply filters with new market type
        applyCurrentFiltersToLoadedProperties()
    }

    fun updatePropertyType(type: String) {
        Log.d(TAG, "Property type changed to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(
            propertyTypes = if (type == "All") emptySet() else setOf(type)
        )
        _uiState.value = _uiState.value.copy(
            currentFilters = updatedFilters,
            selectedPropertyType = type
        )
        // Re-apply filters
        applyCurrentFiltersToLoadedProperties()
    }

    fun updateMinPrice(price: Int) {
        Log.d(TAG, "Min price changed to: $price")
        val updatedFilters = _uiState.value.currentFilters.copy(minPrice = price)
        _uiState.value = _uiState.value.copy(
            currentFilters = updatedFilters,
            minPrice = price
        )
        // Re-apply filters
        applyCurrentFiltersToLoadedProperties()
    }

    /**
     * Apply current filters to already loaded properties without re-fetching from repository
     */
    private fun applyCurrentFiltersToLoadedProperties() {
        viewModelScope.launch {
            try {
                // Get fresh data from repository
                val properties = repository.getAllProperties()

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(
                        isFavorite = savedPropertiesRepository.isPropertySaved(property.id)
                    )
                }

                // Apply filters
                val filteredProperties = applyFiltersToList(propertiesWithFavoriteStatus)

                _uiState.value = _uiState.value.copy(
                    properties = filteredProperties,
                    error = if (filteredProperties.isEmpty()) "No properties match your filters" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error applying filters", e)
            }
        }
    }

    /**
     * Apply all filters to a list of properties
     */
    private fun applyFiltersToList(properties: List<PropertyModel>): List<PropertyModel> {
        val filters = _uiState.value.currentFilters
        var filtered = properties

        Log.d(TAG, "Applying filters to ${properties.size} properties")
        Log.d(TAG, "Filters: $filters")

        // ALWAYS filter by Market Type (Buy/Rent/Book) since it's always visible in UI
        filtered = filtered.filter { property ->
            property.marketType.equals(filters.marketType, ignoreCase = true)
        }
        Log.d(TAG, "After market type filter (${filters.marketType}): ${filtered.size} properties")

        // Only apply other filters if they're explicitly set
        if (!hasActiveFilters()) {
            Log.d(TAG, "No additional filters set, returning ${filtered.size} properties")
            return filtered
        }

        // Property Types
        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property type filter: ${filtered.size} properties")
        }

        // Price Range
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                val minPriceValue = filters.minPrice * 1000
                val maxPriceValue = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE

                priceValue >= minPriceValue && priceValue <= maxPriceValue
            }
            Log.d(TAG, "After price filter: ${filtered.size} properties")
        }

        // Bedrooms
        if (filters.bedrooms.isNotEmpty()) {
            filtered = filtered.filter { property ->
                when (filters.bedrooms) {
                    "Studio" -> property.bedrooms == 0
                    "6+" -> property.bedrooms >= 6
                    else -> property.bedrooms == filters.bedrooms.toIntOrNull()
                }
            }
            Log.d(TAG, "After bedrooms filter: ${filtered.size} properties")
        }

        // Furnishing
        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing filter: ${filtered.size} properties")
        }

        // Parking
        filters.parking?.let { parkingRequired ->
            filtered = filtered.filter { property ->
                property.parking == parkingRequired
            }
            Log.d(TAG, "After parking filter: ${filtered.size} properties")
        }

        // Pets Allowed
        filters.petsAllowed?.let { petsRequired ->
            filtered = filtered.filter { property ->
                property.petsAllowed == petsRequired
            }
            Log.d(TAG, "After pets filter: ${filtered.size} properties")
        }

        // Amenities
        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities filter: ${filtered.size} properties")
        }

        // Floor
        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor filter: ${filtered.size} properties")
        }

        Log.d(TAG, "Final filtered result: ${filtered.size} properties")
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