package com.example.gharbato.data.repository

import com.example.gharbato.data.model.PropertyModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

class PropertyRepositoryImpl : PropertyRepository {

    // Sample data - In real app, this would be from API/Firebase
    private val sampleProperties = listOf(
        PropertyModel(
            id = 1,
            title = "Modern Apartment",
            developer = "Ram Sharma",
            price = "from 8.7 lakh",
            sqft = "1200 sq.ft",
            bedrooms = 2,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800",
            location = "Kathmandu",
            latLng = LatLng(27.7172, 85.3240),
            propertyType = "Apartment",
            floor = "5th Floor",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 2,
            title = "Luxury Villa",
            developer = "Shyam Builders",
            price = "from 15.8 lakh",
            sqft = "2500 sq.ft",
            bedrooms = 3,
            bathrooms = 3,
            imageUrl = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800",
            location = "Lalitpur",
            latLng = LatLng(27.6710, 85.3240),
            propertyType = "Villa",
            floor = "Ground Floor",
            furnishing = "Semi Furnished",
            parking = true,
            petsAllowed = true
        ),
        PropertyModel(
            id = 3,
            title = "Cozy House",
            developer = "Krishna Estates",
            price = "from 12.6 lakh",
            sqft = "1800 sq.ft",
            bedrooms = 3,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800",
            location = "Bhaktapur",
            latLng = LatLng(27.6710, 85.4298),
            propertyType = "House",
            floor = "2 Storey",
            furnishing = "Unfurnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 4,
            title = "Penthouse Suite",
            developer = "Hari Properties",
            price = "13.2 lakh",
            sqft = "2000 sq.ft",
            bedrooms = 2,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
            location = "Pokhara",
            latLng = LatLng(28.2096, 83.9856),
            propertyType = "Penthouse",
            floor = "Top Floor",
            furnishing = "Fully Furnished",
            parking = true,
            petsAllowed = false
        ),
        PropertyModel(
            id = 5,
            title = "Family Home",
            developer = "Sita Developers",
            price = "from 10.5 lakh",
            sqft = "1500 sq.ft",
            bedrooms = 3,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800",
            location = "Kathmandu",
            latLng = LatLng(27.7000, 85.3200),
            propertyType = "House",
            floor = "1 Storey",
            furnishing = "Semi Furnished",
            parking = false,
            petsAllowed = true
        )
    )

    override suspend fun getAllProperties(): List<PropertyModel> {
        // Simulate network delay
        delay(500)
        return sampleProperties
    }

    override suspend fun getPropertyById(id: Int): PropertyModel? {
        delay(300)
        return sampleProperties.find { it.id == id }
    }

    override suspend fun searchProperties(query: String): List<PropertyModel> {
        delay(400)
        return if (query.isEmpty()) {
            sampleProperties
        } else {
            sampleProperties.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true) ||
                        it.developer.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun filterProperties(
        marketType: String,
        propertyType: String,
        minPrice: Int,
        bedrooms: Int
    ): List<PropertyModel> {
        delay(400)
        return sampleProperties.filter { property ->
            // Add your filtering logic here
            true // For now, return all
        }
    }
}