package com.example.gharbato.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PropertyModel(
    val id: Int = 0,
    val title: String = "",
    val developer: String = "",
    val price: String = "",
    val sqft: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val images: Map<String, List<String>> = emptyMap(),
    val location: String = "",

    // Firebase stores lat/lng separately, not LatLng object
    val latitude: Double = 27.7172,
    val longitude: Double = 85.3240,

    val propertyType: String = "Apartment",
    val floor: String = "3rd Floor",
    val furnishing: String = "Fully Furnished",
    val parking: Boolean = true,
    val petsAllowed: Boolean = false,
    var isFavorite: Boolean = false,

    var description: String? = "",
    var utilitiesIncluded: String? = "",
    var commission: String? = "",
    var advancePayment: String? = "",
    var securityDeposit: String? = "",
    var minimumLease: String? = "",
    var availableFrom: String? = "",
    var amenities: List<String> = emptyList()
) {
    //Computed property for LatLng (not stored in Firebase)
    @get:Exclude
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    // Get first image URL
    @get:Exclude
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""
}