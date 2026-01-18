package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.PropertyRepo
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.SortOption
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

private const val TAG = "PropertyViewModel"
private const val SEARCH_DEBOUNCE_DELAY = 300L // milliseconds

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val allLoadedProperties: List<PropertyModel> = emptyList(),
    val selectedProperty: PropertyModel? = null,
    val similarProperties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSimilar: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "",
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

    private var searchJob: Job? = null

    init {
        loadProperties()
        observeSavedProperties()
    }

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Loading properties...")
                val properties = repository.getAllApprovedProperties()

                val propertiesWithFavorites = properties.map { property ->
                    property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                }

                Log.d(TAG, "Loaded ${propertiesWithFavorites.size} properties")

                _uiState.value = _uiState.value.copy(
                    allLoadedProperties = propertiesWithFavorites

                )

                applyCurrentFiltersAndSort()

                _uiState.value = _uiState.value.copy(isLoading = false)

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

                val updatedAllProperties = _uiState.value.allLoadedProperties.map { property ->
                    property.copy(isFavorite = savedIds.contains(property.id))
                }

                val updatedSelectedProperty = _uiState.value.selectedProperty?.let { selected ->
                    selected.copy(isFavorite = savedIds.contains(selected.id))
                }

                _uiState.value = _uiState.value.copy(
                    properties = updatedProperties,
                    allLoadedProperties = updatedAllProperties,
                    selectedProperty = updatedSelectedProperty
                )
            }
        }
    }

    /**
     * Gets a specific property by ID
     */
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

    /**
     * Updates search query and triggers debounced search
     */
    fun updateSearchQuery(query: String) {
        Log.d(TAG, "Search query updated: '$query'")

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchLocation = null // Clear location search when typing text search
        )

        // Cancel previous search job
        searchJob?.cancel()

        // If query is empty, clear search immediately
        if (query.isEmpty()) {
            viewModelScope.launch {
                applyCurrentFiltersAndSort()
            }
            return
        }

        // Debounce search - wait for user to stop typing
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY)
            performSearch()
        }
    }

    /**
     * Performs text-based search on properties
     * Called automatically after debounce delay or manually
     */
    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()

        Log.d(TAG, "Performing search: '$query'")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                var result = _uiState.value.allLoadedProperties

                // Apply text search if query is not empty
                if (query.isNotEmpty()) {
                    val searchLower = query.lowercase().trim()
                    result = result.filter { property ->
                        property.title.lowercase().contains(searchLower) ||
                                property.location.lowercase().contains(searchLower) ||
                                property.developer.lowercase().contains(searchLower) ||
                                property.propertyType.lowercase().contains(searchLower) ||
                                property.marketType.lowercase().contains(searchLower) ||
                                (property.description?.lowercase()?.contains(searchLower) == true)
                    }
                    Log.d(TAG, "Text search found ${result.size} matches")
                }

                // Apply filters to search results
                result = applyFiltersToList(result)
                Log.d(TAG, "After filters: ${result.size} properties")

                // Apply sorting
                result = applySortToProperties(result, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = result,
                    isLoading = false,
                    error = if (result.isEmpty() && _uiState.value.allLoadedProperties.isNotEmpty())
                        "No properties found matching your search"
                    else null
                )

                Log.d(TAG, "Search completed: ${result.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Search error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    /**
     * Performs location-based search on properties
     */
    fun searchByLocation(latitude: Double, longitude: Double, address: String, radiusKm: Float) {
        Log.d(TAG, "Searching by location: ($latitude, $longitude) within ${radiusKm}km")

        _uiState.value = _uiState.value.copy(
            searchLocation = SearchLocation(latitude, longitude, address, radiusKm),
            searchQuery = address
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                var result = _uiState.value.allLoadedProperties

                // Filter by distance
                result = result.filter { property ->
                    val distance = calculateDistance(latitude, longitude, property.latitude, property.longitude)
                    distance <= radiusKm
                }
                Log.d(TAG, "Location search found ${result.size} properties within radius")

                // Apply filters to location results
                result = applyFiltersToList(result)
                Log.d(TAG, "After filters: ${result.size} properties")

                // Apply sorting
                result = applySortToProperties(result, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = result,
                    isLoading = false,
                    error = if (result.isEmpty() && _uiState.value.allLoadedProperties.isNotEmpty())
                        "No properties found in this area"
                    else null
                )

                Log.d(TAG, "Location search completed: ${result.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Location search error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Location search failed"
                )
            }
        }
    }


    fun clearSearch() {
        Log.d(TAG, "Clearing search")

        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchLocation = null
        )

        viewModelScope.launch {
            applyCurrentFiltersAndSort()
        }
    }


    fun getCurrentFilters(): PropertyFilters {
        return _uiState.value.currentFilters
    }


    fun applyFilters(filters: PropertyFilters) {
        Log.d(TAG, "Applying filters: $filters")

        _uiState.value = _uiState.value.copy(
            currentFilters = filters,
            selectedMarketType = filters.marketType,
            selectedPropertyType = if (filters.propertyTypes.isEmpty()) "All" else filters.propertyTypes.first(),
            minPrice = filters.minPrice
        )

        // Re-apply search with new filters
        if (_uiState.value.searchLocation != null) {
            val loc = _uiState.value.searchLocation!!
            searchByLocation(loc.latitude, loc.longitude, loc.address, loc.radiusKm)
        } else if (_uiState.value.searchQuery.isNotEmpty()) {
            performSearch()
        } else {
            applyCurrentFiltersAndSort()
        }
    }

    /**
     * Updates market type filter (Buy/Rent/Book)
     */
    fun updateMarketType(type: String) {
        Log.d(TAG, "Updating market type to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(marketType = type)
        applyFilters(updatedFilters)
    }

    /**
     * Updates property type filter
     */
    fun updatePropertyType(type: String) {
        Log.d(TAG, "Updating property type to: $type")
        val updatedFilters = _uiState.value.currentFilters.copy(
            propertyTypes = if (type == "All") emptySet() else setOf(type)
        )
        applyFilters(updatedFilters)
    }

    /**
     * Updates minimum price filter
     */
    fun updateMinPrice(price: Int) {
        Log.d(TAG, "Updating min price to: $price")
        val updatedFilters = _uiState.value.currentFilters.copy(minPrice = price)
        applyFilters(updatedFilters)
    }


    private fun applyCurrentFiltersAndSort() {
        Log.d(TAG, "Applying current filters and sort")

        var result = _uiState.value.allLoadedProperties
        Log.d(TAG, "Starting with ${result.size} properties")

        // Apply filters
        result = applyFiltersToList(result)
        Log.d(TAG, "After filters: ${result.size} properties")

        // Apply sorting
        result = applySortToProperties(result, _uiState.value.currentSort)

        _uiState.value = _uiState.value.copy(
            properties = result,
            error = if (result.isEmpty() && _uiState.value.allLoadedProperties.isNotEmpty())
                "No properties match your filters"
            else null
        )

        Log.d(TAG, "Filter and sort completed: ${result.size} results")
    }


    private fun applyFiltersToList(properties: List<PropertyModel>): List<PropertyModel> {
        val filters = _uiState.value.currentFilters
        var filtered = properties

        // FIXED: Market Type Filter (only apply if explicitly set and not empty)
        if (filters.marketType.isNotBlank() && filters.marketType != "All") {
            filtered = filtered.filter { property ->
                property.marketType.trim().equals(filters.marketType.trim(), ignoreCase = true)
            }
        }

        // Property Types Filter
        if (filters.propertyTypes.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.propertyTypes.any { type ->
                    property.propertyType.trim().equals(type.trim(), ignoreCase = true)
                }
            }
        }

        // Price Range Filter
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            filtered = filtered.filter { property ->
                val price = extractPriceValue(property.price)
                val min = filters.minPrice * 1000
                val max = if (filters.maxPrice > 0) filters.maxPrice * 1000 else Int.MAX_VALUE
                price in min..max
            }
        }

        // Area Range Filter
        if (filters.minArea > 0 || filters.maxArea > 0) {
            filtered = filtered.filter { property ->
                val area = extractAreaValue(property.sqft)
                val min = filters.minArea
                val max = if (filters.maxArea > 0) filters.maxArea else Int.MAX_VALUE
                area in min..max
            }
        }

        // Bedrooms Filter
        if (filters.bedrooms.isNotEmpty()) {
            filtered = filtered.filter { property ->
                when (filters.bedrooms) {
                    "Studio" -> property.bedrooms == 0
                    "6+" -> property.bedrooms >= 6
                    else -> property.bedrooms == filters.bedrooms.toIntOrNull()
                }
            }
        }

        // Furnishing Filter
        if (filters.furnishing.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.furnishing.trim().equals(filters.furnishing.trim(), ignoreCase = true)
            }
        }

        // Parking Filter
        filters.parking?.let { required ->
            filtered = filtered.filter { it.parking == required }
        }

        // Pets Filter
        filters.petsAllowed?.let { required ->
            filtered = filtered.filter { it.petsAllowed == required }
        }

        // Amenities Filter (must have all selected amenities)
        if (filters.amenities.isNotEmpty()) {
            filtered = filtered.filter { property ->
                filters.amenities.all { amenity ->
                    property.amenities.any { it.trim().equals(amenity.trim(), ignoreCase = true) }
                }
            }
        }

        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.trim().equals(filters.floor.trim(), ignoreCase = true)
            }
        }

        return filtered
    }

    fun getAllPropertiesWithoutFilters(): List<PropertyModel> {
        return _uiState.value.allLoadedProperties
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            currentFilters = PropertyFilters(),
            selectedMarketType = "",
            selectedPropertyType = "All",
            minPrice = 0,
            searchQuery = "",
            searchLocation = null
        )
        applyCurrentFiltersAndSort()
    }

    private fun extractPriceValue(priceString: String): Int {
        return priceString.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    /**
     * Calculates distance between two coordinates in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c).toFloat()
    }

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
                Log.e(TAG, "Error toggling favorite", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }


    fun updateSort(sortOption: SortOption) {
        Log.d(TAG, "Updating sort to: $sortOption")
        val sorted = applySortToProperties(_uiState.value.properties, sortOption)
        _uiState.value = _uiState.value.copy(
            properties = sorted,
            currentSort = sortOption
        )
    }


     //Applies sorting to properties list

    private fun applySortToProperties(
        properties: List<PropertyModel>,
        sortOption: SortOption
    ): List<PropertyModel> {
        return when (sortOption) {
            SortOption.POPULARITY -> properties.sortedByDescending { it.totalViews }
            SortOption.PRICE_LOW_TO_HIGH -> properties.sortedBy { extractPriceValue(it.price) }
            SortOption.PRICE_HIGH_TO_LOW -> properties.sortedByDescending { extractPriceValue(it.price) }
            SortOption.AREA_SMALL_TO_LARGE -> properties.sortedBy { extractAreaValue(it.sqft) }
            SortOption.AREA_LARGE_TO_SMALL -> properties.sortedByDescending { extractAreaValue(it.sqft) }
            SortOption.DATE_NEWEST -> properties.sortedByDescending { it.createdAt }
            SortOption.DATE_OLDEST -> properties.sortedBy { it.createdAt }
        }
    }

    fun loadSimilarProperties(property: PropertyModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSimilar = true)

            try {
                val similar = repository.getSimilarProperties(
                    currentProperty = property,
                    limit = 10
                )

                // Update favorite status for similar properties
                val similarWithFavorites = similar.map { prop ->
                    prop.copy(isFavorite = savedPropertiesRepository.isPropertySaved(prop.id))
                }

                _uiState.value = _uiState.value.copy(
                    similarProperties = similarWithFavorites,
                    isLoadingSimilar = false
                )

                Log.d("PropertyViewModel", "Loaded ${similarWithFavorites.size} similar properties")
            } catch (e: Exception) {
                Log.e("PropertyViewModel", "Error loading similar properties: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    similarProperties = emptyList(),
                    isLoadingSimilar = false
                )
            }
        }
    }

    fun clearSimilarProperties() {
        _uiState.value = _uiState.value.copy(
            similarProperties = emptyList(),
            isLoadingSimilar = false
        )
    }


    private fun extractAreaValue(areaString: String): Int {
        return areaString.filter { it.isDigit() }.toIntOrNull() ?: 0
    }


    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }

    fun uploadImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
        repository.uploadImage(context, imageUri, callback)
    }
}