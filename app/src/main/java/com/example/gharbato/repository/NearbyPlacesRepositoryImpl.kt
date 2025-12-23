package com.example.gharbato.data.repository

import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.data.model.PlaceType
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlin.math.*

class NearbyPlacesRepositoryImpl : NearbyPlacesRepository {

    override suspend fun getNearbyPlaces(location: LatLng): Map<PlaceType, List<NearbyPlace>> {
        delay(300) // Simulate API call

        return mapOf(
            PlaceType.SCHOOL to generateSchools(location),
            PlaceType.HOSPITAL to generateHospitals(location),
            PlaceType.STORE to generateStores(location),
            PlaceType.PARK to generateParks(location),
            PlaceType.RESTAURANT to generateRestaurants(location),
            PlaceType.TRANSPORT to generateTransport(location)
        )
    }

    override suspend fun getPlacesByType(location: LatLng, type: PlaceType): List<NearbyPlace> {
        delay(200)
        return when (type) {
            PlaceType.SCHOOL -> generateSchools(location)
            PlaceType.HOSPITAL -> generateHospitals(location)
            PlaceType.STORE -> generateStores(location)
            PlaceType.PARK -> generateParks(location)
            PlaceType.RESTAURANT -> generateRestaurants(location)
            PlaceType.TRANSPORT -> generateTransport(location)
        }
    }

    private fun generateSchools(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("school_1", "Sunrise Primary School", location, 0.003, 0.004, PlaceType.SCHOOL),
            createPlace("school_2", "International Academy", location, -0.005, 0.003, PlaceType.SCHOOL),
            createPlace("school_3", "City High School", location, 0.007, -0.002, PlaceType.SCHOOL)
        )
    }

    private fun generateHospitals(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("hospital_1", "City Hospital", location, 0.008, -0.004, PlaceType.HOSPITAL),
            createPlace("hospital_2", "Medical Clinic", location, -0.004, 0.006, PlaceType.HOSPITAL),
            createPlace("hospital_3", "Health Center", location, 0.006, 0.005, PlaceType.HOSPITAL)
        )
    }

    private fun generateStores(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("store_1", "Metro Supermarket", location, -0.002, -0.003, PlaceType.STORE),
            createPlace("store_2", "Shopping Mall", location, 0.005, -0.005, PlaceType.STORE),
            createPlace("store_3", "Corner Store", location, 0.001, 0.002, PlaceType.STORE),
            createPlace("store_4", "Grocery Market", location, -0.003, 0.004, PlaceType.STORE)
        )
    }

    private fun generateParks(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("park_1", "Central Park", location, -0.006, 0.004, PlaceType.PARK),
            createPlace("park_2", "Community Garden", location, 0.004, 0.003, PlaceType.PARK)
        )
    }

    private fun generateRestaurants(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("restaurant_1", "The Golden Spoon", location, 0.002, -0.002, PlaceType.RESTAURANT),
            createPlace("restaurant_2", "Pizza Corner", location, -0.003, -0.004, PlaceType.RESTAURANT),
            createPlace("restaurant_3", "Cafe Delight", location, 0.001, 0.003, PlaceType.RESTAURANT),
            createPlace("restaurant_4", "Local Diner", location, 0.004, -0.003, PlaceType.RESTAURANT)
        )
    }

    private fun generateTransport(location: LatLng): List<NearbyPlace> {
        return listOf(
            createPlace("transport_1", "Bus Station", location, -0.005, -0.002, PlaceType.TRANSPORT),
            createPlace("transport_2", "Metro Station", location, 0.006, 0.005, PlaceType.TRANSPORT),
            createPlace("transport_3", "Taxi Stand", location, 0.002, 0.004, PlaceType.TRANSPORT)
        )
    }

    private fun createPlace(
        id: String,
        name: String,
        baseLocation: LatLng,
        latOffset: Double,
        lngOffset: Double,
        type: PlaceType
    ): NearbyPlace {
        val location = LatLng(
            baseLocation.latitude + latOffset,
            baseLocation.longitude + lngOffset
        )
        val distance = calculateDistance(baseLocation, location)

        return NearbyPlace(
            id = id,
            name = name,
            location = location,
            type = type,
            distance = distance,
            formattedDistance = formatDistance(distance)
        )
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
            else -> String.format("%.1fkm", distanceInMeters / 1000)
        }
    }
}