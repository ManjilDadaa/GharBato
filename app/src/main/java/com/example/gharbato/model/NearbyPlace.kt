package com.example.gharbato.model

import com.example.gharbato.data.model.PlaceType
import com.google.android.gms.maps.model.LatLng

data class NearbyPlace(
    val id: String,
    val name: String,
    val location: LatLng,
    val type: PlaceType,
    val distance: Double,
    val formattedDistance: String
)
