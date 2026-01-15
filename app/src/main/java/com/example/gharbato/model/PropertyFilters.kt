package com.example.gharbato.model

data class PropertyFilters(
    val marketType: String = "Buy",
    val rentalPeriod: String = "Long-term",
    val propertyTypes: Set<String> = emptySet(),
    val minPrice: Int = 0,
    val maxPrice: Int = 0,
    val minArea: Int = 0,  // NEW: Minimum area in sq.ft
    val maxArea: Int = 0,  // NEW: Maximum area in sq.ft
    val bedrooms: String = "",
    val furnishing: String = "",
    val parking: Boolean? = null,
    val petsAllowed: Boolean? = null,
    val amenities: Set<String> = emptySet(),
    val floor: String = ""
)
