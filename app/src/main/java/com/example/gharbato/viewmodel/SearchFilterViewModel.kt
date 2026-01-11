package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.LocationFilter
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.SearchFilterState
import com.example.gharbato.repository.SearchFilterRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SearchFilterViewModel"

class SearchFilterViewModel(
    private val searchFilterRepo: SearchFilterRepo
) : ViewModel() {

    private val _state = MutableStateFlow(SearchFilterState())
    val state: StateFlow<SearchFilterState> = _state.asStateFlow()

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    suspend fun applyTextSearch(
        properties: List<PropertyModel>,
        query: String
    ): List<PropertyModel> {
        Log.d(TAG, "Applying text search: '$query'")

        _state.value = _state.value.copy(
            searchQuery = query,
            isSearchActive = query.isNotEmpty()
        )

        return if (query.isEmpty()) {
            properties
        } else {
            searchFilterRepo.searchByText(properties, query)
        }
    }

    suspend fun applyLocationSearch(
        properties: List<PropertyModel>,
        latitude: Double,
        longitude: Double,
        address: String,
        radiusKm: Float
    ): List<PropertyModel> {
        Log.d(TAG, "Applying location search: $address")

        val locationFilter = LocationFilter(latitude, longitude, address, radiusKm)
        _state.value = _state.value.copy(
            locationFilter = locationFilter,
            searchQuery = address,
            isSearchActive = true
        )

        return searchFilterRepo.filterByLocation(
            properties, latitude, longitude, radiusKm
        )
    }

    suspend fun applyFilters(
        properties: List<PropertyModel>,
        filters: PropertyFilters
    ): List<PropertyModel> {
        Log.d(TAG, "Applying filters: $filters")

        val hasFilters = searchFilterRepo.hasActiveFilters(filters)
        _state.value = _state.value.copy(
            activeFilters = filters,
            hasActiveFilters = hasFilters
        )

        return if (hasFilters || filters.marketType.isNotEmpty()) {
            searchFilterRepo.applyFilters(properties, filters)
        } else {
            properties
        }
    }

    fun clearSearch() {
        Log.d(TAG, "Clearing search")
        _state.value = _state.value.copy(
            searchQuery = "",
            isSearchActive = false,
            locationFilter = null
        )
    }

    fun clearFilters() {
        Log.d(TAG, "Clearing filters")
        _state.value = _state.value.copy(
            activeFilters = PropertyFilters(),
            hasActiveFilters = false
        )
    }

    fun getCurrentFilters(): PropertyFilters {
        return _state.value.activeFilters
    }

    fun hasActiveFilters(): Boolean {
        return _state.value.hasActiveFilters
    }
}