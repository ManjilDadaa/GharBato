package com.example.gharbato.model

data class PropertyFilters(
    val marketType: String = "Buy",
    val rentalPeriod: String = "Long-term",
    val propertyTypes: Set<String> = emptySet(),
    val minPrice: Int = 0,
    val maxPrice: Int = 0,
    val bedrooms: String = "",
    val furnishing: String = "",
    val parking: Boolean? = null,
    val petsAllowed: Boolean? = null,
    val amenities: Set<String> = emptySet(),
    val floor: String = ""
)