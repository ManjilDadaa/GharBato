package com.example.gharbato.model

/**
 * Data class to hold a property along with its similarity score
 * Used for ranking similar properties
 */
data class SimilarProperty(
    val property: PropertyModel,
    val similarityScore: Float
)

/**
 * Weights for different similarity factors
 * Higher weight = more important for similarity
 */
object SimilarityWeights {
    const val MARKET_TYPE = 30f
    const val PROPERTY_TYPE = 20f
    const val PRICE_RANGE = 15f
    const val LOCATION = 15f
    const val BEDROOMS = 10f
    const val AREA = 5f
    const val BATHROOMS = 3f
    const val FURNISHING = 2f
}