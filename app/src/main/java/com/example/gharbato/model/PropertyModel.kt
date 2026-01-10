package com.example.gharbato.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * OPTION 1: Keep defaults but add validation
 * This is the SAFEST approach - keeps backward compatibility
 */
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
    val marketType: String = "",

    // Keep defaults for backward compatibility with old Firebase data
    // But now we can detect when they're used
    val latitude: Double = 27.7172,
    val longitude: Double = 85.3240,

    val propertyType: String = "Apartment",
    val floor: String = "3rd Floor",
    val furnishing: String = "Fully Furnished",
    val parking: Boolean = true,
    val petsAllowed: Boolean = false,
    var isFavorite: Boolean = false,
    val ownerId: String = "",
    val ownerName: String = "",
    var description: String? = "",
    var utilitiesIncluded: String? = "",
    var commission: String? = "",
    var advancePayment: String? = "",
    var securityDeposit: String? = "",
    var minimumLease: String? = "",
    var availableFrom: String? = "",
    var amenities: List<String> = emptyList(),

    val status: String = PropertyStatus.PENDING,
    val ownerImageUrl: String = "",
    val ownerEmail: String = "",

    val createdAt: Long = System.currentTimeMillis(),
) {
    // Computed property for LatLng (not stored in Firebase)
    @get:Exclude
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    // Get first image URL
    @get:Exclude
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""

    // ⭐ NEW: Check if coordinates are valid (not default)
    @get:Exclude
    val hasValidCoordinates: Boolean
        get() = !(latitude == 27.7172 && longitude == 85.3240)

    // ⭐ NEW: Check if this is the default Kathmandu location
    @get:Exclude
    val isDefaultLocation: Boolean
        get() = latitude == 27.7172 && longitude == 85.3240
}

object PropertyStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

/**
 * OPTION 2: No defaults - forces explicit coordinates
 * WARNING: This breaks backward compatibility with existing Firebase data!
 * Only use this if you're sure all properties have coordinates.
 */
@IgnoreExtraProperties
data class PropertyModelStrict(
    val id: Int = 0,
    val title: String = "",
    val developer: String = "",
    val price: String = "",
    val sqft: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val images: Map<String, List<String>> = emptyMap(),
    val location: String = "",
    val marketType: String = "",

    // ⚠️ NO DEFAULTS - Must be explicitly provided
    // This will cause Firebase deserialization to fail for old properties
    val latitude: Double,
    val longitude: Double,

    val propertyType: String = "Apartment",
    val floor: String = "3rd Floor",
    val furnishing: String = "Fully Furnished",
    val parking: Boolean = true,
    val petsAllowed: Boolean = false,
    var isFavorite: Boolean = false,
    val ownerId: String = "",
    val ownerName: String = "",
    var description: String? = "",
    var utilitiesIncluded: String? = "",
    var commission: String? = "",
    var advancePayment: String? = "",
    var securityDeposit: String? = "",
    var minimumLease: String? = "",
    var availableFrom: String? = "",
    var amenities: List<String> = emptyList(),

    val status: String = PropertyStatus.PENDING,
    val ownerImageUrl: String = "",
    val ownerEmail: String = "",

    val createdAt: Long = System.currentTimeMillis(),
) {
    @get:Exclude
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    @get:Exclude
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""
}

/**
 * OPTION 3: Nullable coordinates - most flexible
 * Good for gradual migration
 */
@IgnoreExtraProperties
data class PropertyModelNullable(
    val id: Int = 0,
    val title: String = "",
    val developer: String = "",
    val price: String = "",
    val sqft: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val images: Map<String, List<String>> = emptyMap(),
    val location: String = "",
    val marketType: String = "",

    // ⭐ Nullable - explicitly shows when coordinates are missing
    val latitude: Double? = null,
    val longitude: Double? = null,

    val propertyType: String = "Apartment",
    val floor: String = "3rd Floor",
    val furnishing: String = "Fully Furnished",
    val parking: Boolean = true,
    val petsAllowed: Boolean = false,
    var isFavorite: Boolean = false,
    val ownerId: String = "",
    val ownerName: String = "",
    var description: String? = "",
    var utilitiesIncluded: String? = "",
    var commission: String? = "",
    var advancePayment: String? = "",
    var securityDeposit: String? = "",
    var minimumLease: String? = "",
    var availableFrom: String? = "",
    var amenities: List<String> = emptyList(),

    val status: String = PropertyStatus.PENDING,
    val ownerImageUrl: String = "",
    val ownerEmail: String = "",

    val createdAt: Long = System.currentTimeMillis(),
) {
    @get:Exclude
    val latLng: LatLng?
        get() = if (latitude != null && longitude != null) {
            LatLng(latitude, longitude)
        } else {
            null
        }

    @get:Exclude
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""

    @get:Exclude
    val hasValidCoordinates: Boolean
        get() = latitude != null && longitude != null
}