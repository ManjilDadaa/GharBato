package com.example.gharbato.data.repository

import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.data.model.PlaceType
import com.google.android.gms.maps.model.LatLng

interface NearbyPlacesRepository {
    suspend fun getNearbyPlaces(location: LatLng): Map<PlaceType, List<NearbyPlace>>
    suspend fun getPlacesByType(location: LatLng, type: PlaceType): List<NearbyPlace>
}