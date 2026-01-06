package com.example.gharbato.model

/**
 * Enum representing all available sort options for properties
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
}

/**
 * Data class representing sort preferences
 */
data class SortPreferences(
    val currentSort: SortOption = SortOption.DATE_NEWEST,
    val sortAscending: Boolean = true
)

/**
 * Sealed class for sort-related events
 */
sealed class SortEvent {
    data class SortApplied(val sortOption: SortOption) : SortEvent()
    object SortReset : SortEvent()
}

/**
 * Data class for sort state management
 */
data class SortState(
    val availableSorts: List<SortOption> = SortOption.entries,
    val selectedSort: SortOption = SortOption.DATE_NEWEST,
    val isLoading: Boolean = false
)