package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.SortOption
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PropertyViewModel"

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val allProperties: List<PropertyModel> = emptyList(), // Cache of all properties
    val selectedProperty: PropertyModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "Buy",
    val selectedPropertyType: String = "All",
    val minPrice: Int = 0,
    val showMap: Boolean = true,
    val searchLocation: SearchLocation? = null,
    val currentFilters: PropertyFilters = PropertyFilters(),
    val currentSort: SortOption = SortOption.DATE_NEWEST
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

    // ========== PROPERTY LOADING ==========

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Loading all properties...")
                val properties = repository.getAllApprovedProperties()
                Log.d(TAG, "Loaded ${properties.size} properties from repository")

                val propertiesWithFavoriteStatus = properties.map { property ->
                    property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                }

                val sortedProperties = applySortToProperties(
                    propertiesWithFavoriteStatus,
                    _uiState.value.currentSort
                )

                Log.d(TAG, "Initial load - showing all ${sortedProperties.size} properties")

                _uiState.value = _uiState.value.copy(
                    properties = sortedProperties,
                    allProperties = sortedProperties, // Cache all properties
                    isLoading = false,
                    error = if (sortedProperties.isEmpty()) "No properties found" else null
                )
                Log.d(TAG, "UI updated with ${sortedProperties.size} properties")
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

                val updatedAllProperties = _uiState.value.allProperties.map { property ->
                    property.copy(isFavorite = savedIds.contains(property.id))
                }

                val updatedSelectedProperty = _uiState.value.selectedProperty?.let { selected ->
                    selected.copy(isFavorite = savedIds.contains(selected.id))
                }

                _uiState.value = _uiState.value.copy(
                    properties = updatedProperties,
                    allProperties = updatedAllProperties,
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

    // ========== SEARCH ==========

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        Log.d(TAG, "Performing search with query: '$query'")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Start with all cached properties
                var properties = _uiState.value.allProperties

                // If cache is empty, reload
                if (properties.isEmpty()) {
                    properties = repository.getAllApprovedProperties().map { property ->
                        property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                    }
                    _uiState.value = _uiState.value.copy(allProperties = properties)
                }

                Log.d(TAG, "Starting with ${properties.size} properties")

                // Apply search query
                if (query.isNotEmpty()) {
                    val searchQuery = query.lowercase()
                    properties = properties.filter { property ->
                        property.title.lowercase().contains(searchQuery) ||
                                property.location.lowercase().contains(searchQuery) ||
                                property.developer.lowercase().contains(searchQuery) ||
                                property.propertyType.lowercase().contains(searchQuery) ||
                                property.description?.lowercase()?.contains(searchQuery) == true
                    }
                    Log.d(TAG, "After text search: ${properties.size} properties")
                }

                // Apply filters
                properties = applyFiltersToList(properties)
                Log.d(TAG, "After filters: ${properties.size} properties")

                // Apply sorting
                val sortedProperties = applySortToProperties(properties, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = sortedProperties,
                    isLoading = false,
                    error = if (sortedProperties.isEmpty()) "No properties found matching your search" else null
                )

                Log.d(TAG, "Search complete: ${sortedProperties.size} properties displayed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during search", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed"
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
                // Start with all cached properties
                var properties = _uiState.value.allProperties

                if (properties.isEmpty()) {
                    properties = repository.getAllApprovedProperties().map { property ->
                        property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                    }
                    _uiState.value = _uiState.value.copy(allProperties = properties)
                }

                // Apply location filter
                properties = properties.filter { property ->
                    val distance = calculateDistance(
                        latitude, longitude,
                        property.latitude, property.longitude
                    )
                    distance <= radiusKm
                }

                Log.d(TAG, "After location filter: ${properties.size} properties")

                // Apply other filters
                properties = applyFiltersToList(properties)

                // Apply sorting
                val sortedProperties = applySortToProperties(properties, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = sortedProperties,
                    isLoading = false,
                    error = if (sortedProperties.isEmpty()) "No properties found in this area" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during location search", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Location search failed"
                )
            }
        }
    }

    fun clearSearch() {
        Log.d(TAG, "Clearing search and resetting to all properties")

        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchLocation = null
        )

        // Reapply current filters to all properties
        viewModelScope.launch {
            var properties = _uiState.value.allProperties
            properties = applyFiltersToList(properties)
            val sortedProperties = applySortToProperties(properties, _uiState.value.currentSort)

            _uiState.value = _uiState.value.copy(
                properties = sortedProperties,
                error = if (sortedProperties.isEmpty()) "No properties found" else null
            )
        }
    }

    // ========== FILTERS ==========

    fun getCurrentFilters(): PropertyFilters {
        return _uiState.value.currentFilters
    }

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

        // Reapply current search/location with new filters
        if (_uiState.value.searchLocation != null) {
            val location = _uiState.value.searchLocation!!
            searchByLocation(location.latitude, location.longitude, location.address, location.radiusKm)
        } else if (_uiState.value.searchQuery.isNotEmpty()) {
            performSearch()
        } else {
            applyFiltersToAllProperties()
        }
    }

    fun updateMarketType(type: String) {
        Log.d(TAG, "Market type changed to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(marketType = type)
        applyFilters(updatedFilters)
    }

    fun updatePropertyType(type: String) {
        Log.d(TAG, "Property type changed to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(
            propertyTypes = if (type == "All") emptySet() else setOf(type)
        )
        applyFilters(updatedFilters)
    }

    fun updateMinPrice(price: Int) {
        Log.d(TAG, "Min price changed to: $price")
        val updatedFilters = _uiState.value.currentFilters.copy(minPrice = price)
        applyFilters(updatedFilters)
    }

    private fun applyFiltersToAllProperties() {
        viewModelScope.launch {
            try {
                var properties = _uiState.value.allProperties

                if (properties.isEmpty()) {
                    properties = repository.getAllApprovedProperties().map { property ->
                        property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                    }
                    _uiState.value = _uiState.value.copy(allProperties = properties)
                }

                val filteredProperties = applyFiltersToList(properties)
                val sortedProperties = applySortToProperties(filteredProperties, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = sortedProperties,
                    error = if (sortedProperties.isEmpty()) "No properties match your filters" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error applying filters", e)
            }
        }
    }

    private fun applyFiltersToList(properties: List<PropertyModel>): List<PropertyModel> {
        val filters = _uiState.value.currentFilters
        var filtered = properties

        Log.d(TAG, "Applying filters to ${properties.size} properties")

        // Market Type filter (ALWAYS apply)
        filtered = filtered.filter { property ->
            property.marketType.equals(filters.marketType, ignoreCase = true)
        }
        Log.d(TAG, "After market type filter (${filters.marketType}): ${filtered.size} properties")

        // Only apply additional filters if they are set
        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.equals(type, ignoreCase = true)
                }
            }
            Log.d(TAG, "After property type filter: ${filtered.size} properties")
        }

        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val priceValue = extractPriceValue(property.price)
                val minPriceValue = filters.minPrice * 1000
                val maxPriceValue = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                priceValue >= minPriceValue && priceValue <= maxPriceValue
            }
            Log.d(TAG, "After price filter: ${filtered.size} properties")
        }

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

        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.equals(filters.furnishing, ignoreCase = true)
            }
            Log.d(TAG, "After furnishing filter: ${filtered.size} properties")
        }

        filters.parking?.let { parkingRequired ->
            filtered = filtered.filter { it.parking == parkingRequired }
            Log.d(TAG, "After parking filter: ${filtered.size} properties")
        }

        filters.petsAllowed?.let { petsRequired ->
            filtered = filtered.filter { it.petsAllowed == petsRequired }
            Log.d(TAG, "After pets filter: ${filtered.size} properties")
        }

        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
            Log.d(TAG, "After amenities filter: ${filtered.size} properties")
        }

        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.equals(filters.floor, ignoreCase = true)
            }
            Log.d(TAG, "After floor filter: ${filtered.size} properties")
        }

        return filtered
    }

    private fun extractPriceValue(priceString: String): Int {
        val numbers = priceString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
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

    // ========== SORTING ==========

    fun updateSort(sortOption: SortOption) {
        Log.d(TAG, "Updating sort to: $sortOption")
        val sortedProperties = applySortToProperties(_uiState.value.properties, sortOption)
        _uiState.value = _uiState.value.copy(
            properties = sortedProperties,
            currentSort = sortOption
        )
    }

    private fun applySortToProperties(
        properties: List<PropertyModel>,
        sortOption: SortOption
    ): List<PropertyModel> {
        return when (sortOption) {
            SortOption.POPULARITY -> properties.sortedByDescending { it.isFavorite }
            SortOption.PRICE_LOW_TO_HIGH -> properties.sortedBy { extractPriceValue(it.price) }
            SortOption.PRICE_HIGH_TO_LOW -> properties.sortedByDescending { extractPriceValue(it.price) }
            SortOption.AREA_SMALL_TO_LARGE -> properties.sortedBy { extractAreaValue(it.sqft) }
            SortOption.AREA_LARGE_TO_SMALL -> properties.sortedByDescending { extractAreaValue(it.sqft) }
            SortOption.DATE_NEWEST -> properties.sortedByDescending { it.id }
            SortOption.DATE_OLDEST -> properties.sortedBy { it.id }
        }
    }

    private fun extractAreaValue(areaString: String): Int {
        val numbers = areaString.filter { it.isDigit() }
        return numbers.toIntOrNull() ?: 0
    }

    // ========== UI STATE ==========

    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }

    // ========== IMAGE UPLOAD ==========

    fun uploadImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
        repository.uploadImage(context, imageUri, callback)
    }
}