package com.example.gharbato.model

import com.google.android.gms.maps.model.LatLng

data class NearbyPlace(
    val id: String,
    val name: String,
    val location: LatLng,
    val type: PlaceType,
    val distance: Double, // in meters
    val formattedDistance: String // "450m" or "1.2km"
)

enum class PlaceType {
    SCHOOL,
    HOSPITAL,
    STORE,
    PARK,
    RESTAURANT,
    TRANSPORT;

    fun getDisplayName(): String = when (this) {
        SCHOOL -> "Schools"
        HOSPITAL -> "Hospitals"
        STORE -> "Stores"
        PARK -> "Parks"
        RESTAURANT -> "Restaurants"
        TRANSPORT -> "Transport"
    }
}