package com.example.gharbato.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropertyListingState(
    // Step 1: Purpose & Type
    var selectedPurpose: String = "",
    var selectedPropertyType: String = "",

    // Step 2: Details
    var title: String = "",
    var developer: String = "",
    var price: String = "",
    var location: String = "",
    var area: String = "",
    var floor: String = "",
    var furnishing: String = "Fully Furnished",
    var bedrooms: String = "",
    var bathrooms: String = "",
    var parking: Boolean = true,
    var petsAllowed: Boolean = false,
    var description: String = "",
    var kitchen: String = "",
    var totalRooms: String = "",

    // Step 3: Images
    var imageCategories: List<ImageCategory> = getDefaultImageCategories(),

    // Step 4: Rental Terms (only for Rent/Book)
    var utilitiesIncluded: String = "Included (electricity extra)",
    var commission: String = "No commission",
    var advancePayment: String = "1 month rent",
    var securityDeposit: String = "2 months rent",
    var minimumLease: String = "12 months",
    var availableFrom: String = "Immediate",

    // Step 5: Amenities
    var amenities: List<String> = emptyList()
) : Parcelable

// Amenities list
fun getDefaultAmenities(): List<String> {
    return listOf(
        "Air Conditioning",
        "WiFi Internet",
        "Washing Machine",
        "Refrigerator",
        "Security",
        "Elevator",
        "Gym",
        "Swimming Pool",
        "Garden",
        "Balcony",
        "Power Backup",
        "Water Supply 24/7"
    )
}

@Parcelize
data class ImageCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val isRequired: Boolean,
    val maxImages: Int,
    val images: MutableList<String> = mutableListOf()
) : Parcelable

fun getDefaultImageCategories(): List<ImageCategory> {
    return listOf(
        ImageCategory(
            id = "cover",
            title = "Cover Photo",
            description = "Main photo that buyers will see first",
            icon = com.example.gharbato.R.drawable.baseline_image_24,
            isRequired = true,
            maxImages = 1
        ),
        ImageCategory(
            id = "exterior",
            title = "Exterior Views",
            description = "Outside views, facade, entrance",
            icon = com.example.gharbato.R.drawable.home,
            isRequired = false,
            maxImages = 5
        ),
        ImageCategory(
            id = "living",
            title = "Living Areas",
            description = "Living room, dining room, hall",
            icon = com.example.gharbato.R.drawable.outline_person_24,
            isRequired = false,
            maxImages = 5
        ),
        ImageCategory(
            id = "bedrooms",
            title = "Bedrooms",
            description = "All bedroom photos",
            icon = com.example.gharbato.R.drawable.baseline_bedroom_child_24,
            isRequired = true,
            maxImages = 10
        ),
        ImageCategory(
            id = "bathrooms",
            title = "Bathrooms",
            description = "Bathroom and toilet photos",
            icon = com.example.gharbato.R.drawable.baseline_bathroom_24,
            isRequired = false,
            maxImages = 5
        ),
        ImageCategory(
            id = "kitchen",
            title = "Kitchen",
            description = "Kitchen and dining area",
            icon = com.example.gharbato.R.drawable.baseline_kitchen_24,
            isRequired = false,
            maxImages = 5
        ),
        ImageCategory(
            id = "other",
            title = "Other Spaces",
            description = "Balcony, terrace, parking, etc.",
            icon = com.example.gharbato.R.drawable.baseline_more_24,
            isRequired = false,
            maxImages = 8
        )
    )
}