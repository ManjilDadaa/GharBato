package com.example.gharbato.data.model

import com.google.android.gms.maps.model.LatLng

data class PropertyModel(
    val id: Int,
    val title: String,
    val developer: String,
    val price: String,
    val sqft: String,
    val bedrooms: Int,
    val bathrooms: Int,
    val images: Map<String, List<String>>,
    val location: String,
    val latLng: LatLng,
    val propertyType: String = "Apartment",
    val floor: String = "3rd Floor",
    val furnishing: String = "Fully Furnished",
    val parking: Boolean = true,
    val petsAllowed: Boolean = false,
    var isFavorite: Boolean = false
){
    val imageUrl: String
        get() = images["cover"]?.firstOrNull()
            ?: images.values.flatten().firstOrNull()
            ?: ""
}


// Sample data
object SampleData {
    val properties = listOf(
        PropertyModel(
            id = 1,
            title = "Luxury Villa",
            developer = "ABC Builders",
            price = "Rs 2,50,00,000",
            sqft = "2500 sq.ft",
            bedrooms = 4,
            bathrooms = 3,

            images = mapOf(
                "cover" to listOf(
                    "https://picsum.photos/600/400"
                ),
                "bedrooms" to listOf(
                    "https://picsum.photos/600/401",
                    "https://picsum.photos/600/402"
                ),
                "kitchen" to listOf(
                    "https://picsum.photos/600/403"
                )
            ),

            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "House",
            floor = "2",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = true
        ),
        PropertyModel(
            id = 1,
            title = "Luxury Villa",
            developer = "ABC Builders",
            price = "Rs 2,50,00,000",
            sqft = "2500 sq.ft",
            bedrooms = 4,
            bathrooms = 3,

            images = mapOf(
                "cover" to listOf(
                    "https://picsum.photos/600/400"
                ),
                "bedrooms" to listOf(
                    "https://picsum.photos/600/401",
                    "https://picsum.photos/600/402"
                ),
                "kitchen" to listOf(
                    "https://picsum.photos/600/403"
                )
            ),

            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "House",
            floor = "2",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = true
        )

    )
}