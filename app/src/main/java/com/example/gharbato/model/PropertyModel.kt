package com.example.gharbato.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.*

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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
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
    val updatedAt: Long = System.currentTimeMillis(),

    val totalViews: Int = 0,
    val todayViews: Int = 0,
    val uniqueViewers: Int = 0,
    val lastViewedAt: Long = 0,
    val viewerIds: Map<String, Long> = emptyMap(),

    var firebaseKey: String? = null,
    val kitchen: String = "",
    val totalRooms: String = "",
    val propertyStatus: String = "AVAILABLE"
) {
    @get:Exclude
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    @get:Exclude
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""

    @get:Exclude
    val hasValidCoordinates: Boolean
        get() = !(latitude == 27.7172 && longitude == 85.3240)

    @get:Exclude
    val isDefaultLocation: Boolean
        get() = latitude == 27.7172 && longitude == 85.3240


    @get:Exclude
    val formattedUpdatedTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - updatedAt

            return when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000} minutes ago" // Less than 1 hour
                diff < 86400000 -> { // Less than 24 hours
                    val hours = diff / 3600000
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
                diff < 172800000 -> "Yesterday" // Less than 2 days
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                    sdf.format(Date(updatedAt))
                }
            }
        }

    @get:Exclude
    val viewsText: String
        get() = "$totalViews views, $todayViews today"

    @get:Exclude
    val uniqueViewersText: String
        get() = "$uniqueViewers unique visitors"
}

object PropertyStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}