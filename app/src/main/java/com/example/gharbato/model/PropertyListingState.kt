package com.example.gharbato.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropertyListingState(
    // Step 1: Purpose & Type
    var selectedPurpose: String = "",
    var selectedPropertyType: String = "",

    // Step 2: Details
    var title: String = "",
    var price: String = "",
    var location: String = "",
    var bedrooms: String = "",
    var bathrooms: String = "",
    var area: String = "",
    var description: String = "",
    var kitchen: String = "",
    var totalRooms: String = "",

    // Step 3: Images
    var imageCategories: List<ImageCategory> = getDefaultImageCategories()
) : Parcelable

@Parcelize
data class ImageCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val isRequired: Boolean,
    val maxImages: Int,
    val images: MutableList<String> = mutableListOf() // Store as URI strings for Parcelable
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