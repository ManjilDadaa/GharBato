package com.example.gharbato.model

data class SearchFilterState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val activeFilters: PropertyFilters = PropertyFilters(),
    val hasActiveFilters: Boolean = false,
    val locationFilter: LocationFilter? = null
)

data class LocationFilter(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
)

data class FilterCriteria(
    val marketType: String? = null,
    val propertyTypes: Set<String> = emptySet(),
    val priceRange: PriceRange? = null,
    val bedrooms: String? = null,
    val furnishing: String? = null,
    val parking: Boolean? = null,
    val petsAllowed: Boolean? = null,
    val amenities: Set<String> = emptySet(),
    val floor: String? = null
)

data class PriceRange(
    val min: Int,
    val max: Int
)

sealed class SearchFilterAction {
    data class UpdateSearchQuery(val query: String) : SearchFilterAction()
    data class ApplyTextSearch(val query: String) : SearchFilterAction()
    data class ApplyLocationSearch(val filter: LocationFilter) : SearchFilterAction()
    data class ApplyFilters(val filters: PropertyFilters) : SearchFilterAction()
    object ClearSearch : SearchFilterAction()
    object ClearFilters : SearchFilterAction()
}