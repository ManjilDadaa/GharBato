package com.example.gharbato.data.model

import com.google.android.gms.maps.model.LatLng

data class SearchUiState(
    val searchQuery: String = "",
    val searchLocation: SearchLocation? = null,
    val properties: List<PropertyModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val selectedProperty: PropertyModel? = null,
    val selectedMarketType: String = "Rent",
    val selectedPropertyType: String = "All",
    val minPrice: Int = 10
)

data class SearchLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
)

// Make sure PropertyModel has latitude and longitude
// If not already present, add these fields to your PropertyModel:
/*
data class PropertyModel(
    val id: Int = 0,
    val title: String = "",
    val location: String = "",
    val developer: String = "",
    val propertyType: String = "",
    val marketType: String = "",
    val price: String = "",
    val description: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val sqft: String = "",
    val floor: String = "",
    val furnishing: String = "",
    val parking: Boolean = false,
    val petsAllowed: Boolean = false,
    val imageUrl: String = "",
    val images: Map<String, List<String>> = emptyMap(),
    val isFavorite: Boolean = false,
    val latLng: LatLng = LatLng(27.7172, 85.3240),
    val ownerId: String = "",
    val ownerName: String = ""
)
*/