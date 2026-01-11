package com.example.gharbato.util

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.example.gharbato.model.PropertyListingState
import com.google.firebase.auth.FirebaseAuth

/**
 * Helper object to convert PropertyListingState to PropertyModel
 */
object PropertyListingHelper {

    /**
     * Converts PropertyListingState to PropertyModel
     * @param state The property listing state from the form
     * @param uploadedImages Map of image categories to their uploaded URLs
     * @return PropertyModel ready to be saved to Firebase
     */
    fun stateToModel(
        state: PropertyListingState,
        uploadedImages: Map<String, List<String>>
    ): PropertyModel {
        val currentUser = FirebaseAuth.getInstance().currentUser

        return PropertyModel(
            id = 0, // Will be set by repository
            title = state.title,
            developer = state.developer,
            price = formatPrice(state.price, state.selectedPurpose),
            sqft = "${state.area} sq ft",
            bedrooms = state.bedrooms.toIntOrNull() ?: 0,
            bathrooms = state.bathrooms.toIntOrNull() ?: 0,
            images = uploadedImages,
            location = state.location,
            marketType = state.selectedPurpose,

            // Location coordinates from map picker
            latitude = state.latitude,
            longitude = state.longitude,

            propertyType = state.selectedPropertyType,
            floor = state.floor,
            furnishing = state.furnishing,
            parking = state.parking,
            petsAllowed = state.petsAllowed,

            ownerId = currentUser?.uid ?: "",
            ownerName = currentUser?.displayName ?: state.developer,
            ownerEmail = currentUser?.email ?: "",
            ownerImageUrl = currentUser?.photoUrl?.toString() ?: "",

            description = state.description,
            utilitiesIncluded = state.utilitiesIncluded,
            commission = state.commission,
            advancePayment = state.advancePayment,
            securityDeposit = state.securityDeposit,
            minimumLease = state.minimumLease,
            availableFrom = state.availableFrom,
            amenities = state.amenities,

            status = PropertyStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )
    }

    /**
     * Formats the price with currency and period
     */
    private fun formatPrice(price: String, purpose: String): String {
        if (price.isEmpty()) return "NPR 0"

        val cleanPrice = price.replace(",", "").trim()
        val formattedNumber = cleanPrice.toLongOrNull()?.let { num ->
            String.format("%,d", num)
        } ?: cleanPrice

        return when (purpose) {
            "Sell" -> "NPR $formattedNumber"
            "Rent" -> "NPR $formattedNumber/month"
            "Book" -> "NPR $formattedNumber/night"
            else -> "NPR $formattedNumber"
        }
    }

    /**
     * Validates the property listing state
     * @return Pair of isValid and error message (if any)
     */
    fun validateState(state: PropertyListingState): Pair<Boolean, String?> {
        return when {
            state.title.isBlank() -> false to "Please enter property title"
            state.developer.isBlank() -> false to "Please enter owner/developer name"
            state.price.isBlank() -> false to "Please enter price"
            state.location.isBlank() || !state.hasSelectedLocation ->
                false to "Please select property location on map"
            state.area.isBlank() -> false to "Please enter area"
            state.floor.isBlank() -> false to "Please enter floor information"
            state.bedrooms.isBlank() -> false to "Please enter number of bedrooms"
            state.bathrooms.isBlank() -> false to "Please enter number of bathrooms"
            state.description.isBlank() -> false to "Please enter property description"
            else -> true to null
        }
    }

    /**
     * Validates images
     * @return Pair of isValid and error message (if any)
     */
    fun validateImages(state: PropertyListingState): Pair<Boolean, String?> {
        val coverImages = state.imageCategories.find { it.id == "cover" }?.images ?: emptyList()
        val bedroomImages = state.imageCategories.find { it.id == "bedrooms" }?.images ?: emptyList()

        return when {
            coverImages.isEmpty() -> false to "Please upload a cover photo"
            bedroomImages.isEmpty() -> false to "Please upload at least one bedroom photo"
            else -> true to null
        }
    }
}