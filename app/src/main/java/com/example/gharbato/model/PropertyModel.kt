package com.example.gharbato.model

data class PropertyModel(
    val id: Int,
    val title: String,
    val developer: String,
    val price: String,
    val sqft: String,
    val bedrooms: Int,
    val bathrooms: Int,
    val imageUrl: String,
    val location: String

)



// Sample data
object SampleData {
    val properties = listOf(
        PropertyModel(
            id = 1,
            title = "Modern Apartment",
            developer = "Ram Sharma",
            price = "from 8.7 lakh",
            sqft = "1200 sq.ft",
            bedrooms = 2,
            bathrooms = 2,
            imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800",
            location = "Kathmandu"
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
            location = "Lalitpur"
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
            location = "Bhaktapur"
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
            location = "Pokhara"
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
            location = "Kathmandu"
        )
    )
}