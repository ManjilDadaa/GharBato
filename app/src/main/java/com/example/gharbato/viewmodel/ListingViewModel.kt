package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.model.ListingValidationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListingViewModel(
    private val repository: PropertyRepo
) : ViewModel() {

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadProgress = MutableStateFlow("")
    val uploadProgress: StateFlow<String> = _uploadProgress

    private val _uploadSuccess = MutableStateFlow<Boolean?>(null)
    val uploadSuccess: StateFlow<Boolean?> = _uploadSuccess

    // ✅ STEP 1 VALIDATION - Purpose & Property Type
    fun validateStep1(state: PropertyListingState): ListingValidationResult {
        return when {
            state.selectedPurpose.isBlank() -> {
                ListingValidationResult(false, "Please select a purpose (Sell/Rent/Book)")
            }
            state.selectedPropertyType.isBlank() -> {
                ListingValidationResult(false, "Please select a property type")
            }
            else -> ListingValidationResult(true)
        }
    }

    // ✅ STEP 2 VALIDATION - Property Details
    fun validateStep2(state: PropertyListingState): ListingValidationResult {
        return when {
            state.title.isBlank() -> {
                ListingValidationResult(false, "Property title is required")
            }
            state.title.length < 10 -> {
                ListingValidationResult(false, "Property title must be at least 10 characters")
            }
            state.developer.isBlank() -> {
                ListingValidationResult(false, "Owner/Developer name is required")
            }
            state.price.isBlank() -> {
                ListingValidationResult(false, "Price is required")
            }
            state.price.toIntOrNull() == null -> {
                ListingValidationResult(false, "Please enter a valid price")
            }
            state.price.toInt() <= 0 -> {
                ListingValidationResult(false, "Price must be greater than 0")
            }
            state.area.isBlank() -> {
                ListingValidationResult(false, "Area is required")
            }
            state.area.toIntOrNull() == null -> {
                ListingValidationResult(false, "Please enter a valid area")
            }
            state.area.toInt() <= 0 -> {
                ListingValidationResult(false, "Area must be greater than 0")
            }
            state.location.isBlank() -> {
                ListingValidationResult(false, "Location is required")
            }
            state.floor.isBlank() -> {
                ListingValidationResult(false, "Floor information is required")
            }
            state.furnishing.isBlank() -> {
                ListingValidationResult(false, "Please select furnishing type")
            }
            state.bedrooms.isBlank() -> {
                ListingValidationResult(false, "Number of bedrooms is required")
            }
            state.bedrooms.toIntOrNull() == null -> {
                ListingValidationResult(false, "Please enter a valid number of bedrooms")
            }
            state.bathrooms.isBlank() -> {
                ListingValidationResult(false, "Number of bathrooms is required")
            }
            state.bathrooms.toIntOrNull() == null -> {
                ListingValidationResult(false, "Please enter a valid number of bathrooms")
            }
            state.description.isBlank() -> {
                ListingValidationResult(false, "Property description is required")
            }
            state.description.length < 20 -> {
                ListingValidationResult(false, "Description must be at least 20 characters")
            }
            else -> ListingValidationResult(true)
        }
    }

    // ✅ STEP 3 VALIDATION - Photos
    fun validateStep3(state: PropertyListingState): ListingValidationResult {
        val coverPhotos = state.imageCategories.find { it.id == "cover" }?.images ?: emptyList()
        val bedroomPhotos = state.imageCategories.find { it.id == "bedrooms" }?.images ?: emptyList()
        val totalPhotos = state.imageCategories.sumOf { it.images.size }

        return when {
            coverPhotos.isEmpty() -> {
                ListingValidationResult(false, "Cover photo is required")
            }
            bedroomPhotos.isEmpty() -> {
                ListingValidationResult(false, "At least one bedroom photo is required")
            }
            totalPhotos < 3 -> {
                ListingValidationResult(false, "Please add at least 3 photos in total")
            }
            else -> ListingValidationResult(true)
        }
    }

    // ✅ Master validation function
    fun validateStep(step: Int, state: PropertyListingState): ListingValidationResult {
        return when (step) {
            1 -> validateStep1(state)
            2 -> validateStep2(state)
            3 -> validateStep3(state)
            else -> ListingValidationResult(false, "Invalid step")
        }
    }

    fun submitListing(
        context: Context,
        state: PropertyListingState,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uploadProgress.value = "Preparing images..."

                // Collect all image URIs from categories
                val allImageUris = mutableListOf<Uri>()
                state.imageCategories.forEach { category ->
                    category.images.forEach { uriString ->
                        try {
                            allImageUris.add(Uri.parse(uriString))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (allImageUris.isEmpty()) {
                    _uploadProgress.value = "No images selected"
                    createAndSubmitProperty(state, emptyList(), onSuccess, onError)
                    return@launch
                }

                // Upload images to Cloudinary
                _uploadProgress.value = "Uploading ${allImageUris.size} images..."

                repository.uploadMultipleImages(context, allImageUris) { uploadedUrls ->
                    viewModelScope.launch {
                        if (uploadedUrls.isEmpty()) {
                            _isUploading.value = false
                            _uploadSuccess.value = false
                            onError("Failed to upload images. Please check your internet connection.")
                        } else {
                            _uploadProgress.value = "✅ ${uploadedUrls.size} images uploaded. Saving to database..."
                            createAndSubmitProperty(state, uploadedUrls, onSuccess, onError)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _isUploading.value = false
                _uploadSuccess.value = false
                _uploadProgress.value = ""
                onError("Error: ${e.message}")
            }
        }
    }

    private suspend fun createAndSubmitProperty(
        state: PropertyListingState,
        uploadedUrls: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {

            val categorizedImages = mutableMapOf<String, List<String>>()
            var currentIndex = 0

            state.imageCategories.forEach { category ->
                val count = category.images.size
                if (count > 0) {
                    val urlsForCategory = uploadedUrls
                        .drop(currentIndex)
                        .take(count)

                    categorizedImages[category.id] = urlsForCategory
                    currentIndex += count
                }
            }

            val property = PropertyModel(
                id = System.currentTimeMillis().toInt(),
                title = state.title,
                developer = state.developer,
                price = when (state.selectedPurpose) {
                    "Sell" -> "Rs ${state.price}"
                    "Rent" -> "Rs ${state.price}/month"
                    "Book" -> "Rs ${state.price}/night"
                    else -> "Rs ${state.price}"
                },
                sqft = "${state.area} sq.ft",
                bedrooms = state.bedrooms.toIntOrNull() ?: 0,
                bathrooms = state.bathrooms.toIntOrNull() ?: 0,

                images = categorizedImages, // ⭐ HERE

                location = state.location,
                latLng = LatLng(27.7172, 85.3240),
                propertyType = state.selectedPropertyType,
                floor = state.floor,
                furnishing = state.furnishing,
                parking = state.parking,
                petsAllowed = state.petsAllowed,
                isFavorite = false
            )

            _uploadProgress.value = "Saving to database..."

            repository.addProperty(property) { success, error ->
                _isUploading.value = false
                if (success) {
                    _uploadSuccess.value = true
                    _uploadProgress.value = "✅ Listing created successfully!"
                    onSuccess()
                } else {
                    _uploadSuccess.value = false
                    onError(error ?: "Failed to save property")
                }
            }

        } catch (e: Exception) {
            _isUploading.value = false
            onError(e.message ?: "Unknown error")
        }
    }


    fun resetUploadStatus() {
        _uploadSuccess.value = null
        _uploadProgress.value = ""
    }
}