package com.example.gharbato.model

/**
 * Enum representing all available sort options for properties
 * This is the MODEL in MVVM architecture
 */
enum class SortOption(val displayName: String, val description: String) {
    POPULARITY("By Popularity", "Most viewed and favorited properties"),
    PRICE_LOW_TO_HIGH("Price: Low to High", "Cheapest properties first"),
    PRICE_HIGH_TO_LOW("Price: High to Low", "Most expensive properties first"),
    AREA_SMALL_TO_LARGE("Area: Small to Large", "Smallest area first"),
    AREA_LARGE_TO_SMALL("Area: Large to Small", "Largest area first"),
    DATE_NEWEST("Newest First", "Recently added properties"),
    DATE_OLDEST("Oldest First", "Oldest listings first");

    /**
     * Get short display name for compact UI
     */
    fun getShortName(): String {
        return when (this) {
            POPULARITY -> "Popular"
            PRICE_LOW_TO_HIGH -> "Price ↑"
            PRICE_HIGH_TO_LOW -> "Price ↓"
            AREA_SMALL_TO_LARGE -> "Area ↑"
            AREA_LARGE_TO_SMALL -> "Area ↓"
            DATE_NEWEST -> "Newest"
            DATE_OLDEST -> "Oldest"
        }
    }

    /**
     * Get icon description for accessibility
     */
    fun getIconDescription(): String {
        return when (this) {
            POPULARITY -> "Sort by popularity"
            PRICE_LOW_TO_HIGH -> "Sort by price ascending"
            PRICE_HIGH_TO_LOW -> "Sort by price descending"
            AREA_SMALL_TO_LARGE -> "Sort by area ascending"
            AREA_LARGE_TO_SMALL -> "Sort by area descending"
            DATE_NEWEST -> "Sort by newest"
            DATE_OLDEST -> "Sort by oldest"
        }
    }
}