package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
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
import kotlin.math.*

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val allLoadedProperties: List<PropertyModel> = emptyList(),
    val selectedProperty: PropertyModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMarketType: String = "Buy",
    val selectedPropertyType: String = "All",
    val minPrice: Int = 0,
    val showMap: Boolean = true,
    val searchLocation: SearchLocation? = null,
    val currentFilters: PropertyFilters = PropertyFilters(marketType = "Buy"),
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

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val properties = repository.getAllApprovedProperties()

                val propertiesWithFavorites = properties.map { property ->
                    property.copy(isFavorite = savedPropertiesRepository.isPropertySaved(property.id))
                }

                _uiState.value = _uiState.value.copy(
                    allLoadedProperties = propertiesWithFavorites,
                    isLoading = false
                )

                applyCurrentFiltersAndSort()
            } catch (e: Exception) {
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

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                var result = _uiState.value.allLoadedProperties

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
                }

                result = applyFiltersToList(result)
                result = applySortToProperties(result, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = result,
                    isLoading = false,
                    error = if (result.isEmpty()) "No properties found matching your search" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun searchByLocation(latitude: Double, longitude: Double, address: String, radiusKm: Float) {
        _uiState.value = _uiState.value.copy(
            searchLocation = SearchLocation(latitude, longitude, address, radiusKm),
            searchQuery = address
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                var result = _uiState.value.allLoadedProperties

                result = result.filter { property ->
                    val distance = calculateDistance(latitude, longitude, property.latitude, property.longitude)
                    distance <= radiusKm
                }

                result = applyFiltersToList(result)
                result = applySortToProperties(result, _uiState.value.currentSort)

                _uiState.value = _uiState.value.copy(
                    properties = result,
                    isLoading = false,
                    error = if (result.isEmpty()) "No properties found in this area" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Location search failed"
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchLocation = null
        )
        applyCurrentFiltersAndSort()
    }

    fun getCurrentFilters(): PropertyFilters {
        return _uiState.value.currentFilters
    }

    fun applyFilters(filters: PropertyFilters) {
        _uiState.value = _uiState.value.copy(
            currentFilters = filters,
            selectedMarketType = filters.marketType,
            selectedPropertyType = if (filters.propertyTypes.isEmpty()) "All" else filters.propertyTypes.first(),
            minPrice = filters.minPrice
        )

        if (_uiState.value.searchLocation != null) {
            val loc = _uiState.value.searchLocation!!
            searchByLocation(loc.latitude, loc.longitude, loc.address, loc.radiusKm)
        } else if (_uiState.value.searchQuery.isNotEmpty()) {
            performSearch()
        } else {
            applyCurrentFiltersAndSort()
        }
    }

    fun updateMarketType(type: String) {
        val updatedFilters = _uiState.value.currentFilters.copy(marketType = type)
        applyFilters(updatedFilters)
    }

    fun updatePropertyType(type: String) {
        val updatedFilters = _uiState.value.currentFilters.copy(
            propertyTypes = if (type == "All") emptySet() else setOf(type)
        )
        applyFilters(updatedFilters)
    }

    fun updateMinPrice(price: Int) {
        val updatedFilters = _uiState.value.currentFilters.copy(minPrice = price)
        applyFilters(updatedFilters)
    }

    private fun applyCurrentFiltersAndSort() {
        var result = _uiState.value.allLoadedProperties
        result = applyFiltersToList(result)
        result = applySortToProperties(result, _uiState.value.currentSort)

        _uiState.value = _uiState.value.copy(
            properties = result,
            error = if (result.isEmpty()) "No properties match your filters" else null
        )
    }

    private fun applyFiltersToList(properties: List<PropertyModel>): List<PropertyModel> {
        val filters = _uiState.value.currentFilters
        var filtered = properties

        // Market Type Filter (always apply if set)
        if (filters.marketType.isNotBlank()) {
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

        // Floor Filter
        if (filters.floor.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.floor.trim().equals(filters.floor.trim(), ignoreCase = true)
            }
        }

        return filtered
    }

    private fun extractPriceValue(priceString: String): Int {
        return priceString.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371.0
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
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateSort(sortOption: SortOption) {
        val sorted = applySortToProperties(_uiState.value.properties, sortOption)
        _uiState.value = _uiState.value.copy(
            properties = sorted,
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
        return areaString.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(showMap = !_uiState.value.showMap)
    }

    fun uploadImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
        repository.uploadImage(context, imageUri, callback)
    }
}