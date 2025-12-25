package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.data.model.PlaceType
import com.example.gharbato.data.repository.NearbyPlacesRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val propertyLocation: LatLng = LatLng(0.0, 0.0),
    val propertyName: String = "",
    val nearbyPlaces: Map<PlaceType, List<NearbyPlace>> = emptyMap(),
    val selectedFilters: Set<PlaceType> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel(
    private val repository: NearbyPlacesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun initialize(propertyLocation: LatLng, propertyName: String) {
        _uiState.value = _uiState.value.copy(
            propertyLocation = propertyLocation,
            propertyName = propertyName,
            isLoading = true
        )
        loadNearbyPlaces(propertyLocation)
    }

    private fun loadNearbyPlaces(location: LatLng) {
        viewModelScope.launch {
            try {
                val places = repository.getNearbyPlaces(location)
                _uiState.value = _uiState.value.copy(
                    nearbyPlaces = places,
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

    fun toggleFilter(placeType: PlaceType) {
        val currentFilters = _uiState.value.selectedFilters
        _uiState.value = _uiState.value.copy(
            selectedFilters = if (currentFilters.contains(placeType)) {
                currentFilters - placeType
            } else {
                currentFilters + placeType
            }
        )
    }

    fun getFilteredPlaces(): Map<PlaceType, List<NearbyPlace>> {
        val selectedFilters = _uiState.value.selectedFilters
        return if (selectedFilters.isEmpty()) {
            emptyMap()
        } else {
            _uiState.value.nearbyPlaces.filterKeys { it in selectedFilters }
        }
    }

    fun getPlaceCount(placeType: PlaceType): Int {
        return _uiState.value.nearbyPlaces[placeType]?.size ?: 0
    }
}