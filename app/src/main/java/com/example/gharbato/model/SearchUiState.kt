package com.example.gharbato.model

data class PropertyUiState(
    val properties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val searchQuery: String = "",
    val selectedMarketType: String = "",
    val selectedPropertyType: String = "All Types",
    val minPrice: Int = 0,
    val selectedProperty: PropertyModel? = null
)

data class SearchLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
)

