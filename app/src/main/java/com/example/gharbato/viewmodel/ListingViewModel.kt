package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.model.ListingValidationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "ListingViewModel"

class ListingViewModel(
    private val repository: PropertyRepo
) : ViewModel() {

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadProgress = MutableStateFlow("")
    val uploadProgress: StateFlow<String> = _uploadProgress

    private val _uploadSuccess = MutableStateFlow<Boolean?>(null)
    val uploadSuccess: StateFlow<Boolean?> = _uploadSuccess

    // STEP 1 VALIDATION - Purpose & Property Type
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

    // STEP 2 VALIDATION - Property Details
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
            // ⭐ NEW: Validate that location was selected on map
            !state.hasSelectedLocation -> {
                ListingValidationResult(false, "Please select property location on the map")
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

    // STEP 3 VALIDATION - Photos
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

    // STEP 4 VALIDATION - Rental Terms (only for Rent/Book)
    fun validateStep4(state: PropertyListingState): ListingValidationResult {
        if (state.selectedPurpose == "Sell") {
            return ListingValidationResult(true)
        }

        return when {
            state.utilitiesIncluded.isBlank() -> {
                ListingValidationResult(false, "Please select utilities option")
            }
            state.commission.isBlank() -> {
                ListingValidationResult(false, "Please select commission terms")
            }
            state.advancePayment.isBlank() -> {
                ListingValidationResult(false, "Please select advance payment terms")
            }
            state.securityDeposit.isBlank() -> {
                ListingValidationResult(false, "Please select security deposit terms")
            }
            state.minimumLease.isBlank() -> {
                ListingValidationResult(false, "Please select minimum lease period")
            }
            state.availableFrom.isBlank() -> {
                ListingValidationResult(false, "Please select availability date")
            }
            else -> ListingValidationResult(true)
        }
    }

    // STEP 5 VALIDATION - Amenities
    fun validateStep5(state: PropertyListingState): ListingValidationResult {
        return when {
            state.amenities.isEmpty() -> {
                ListingValidationResult(false, "Please select at least one amenity")
            }
            else -> ListingValidationResult(true)
        }
    }

    fun validateStep(step: Int, state: PropertyListingState): ListingValidationResult {
        return when (step) {
            1 -> validateStep1(state)
            2 -> validateStep2(state)
            3 -> validateStep3(state)
            4 -> validateStep4(state)
            5 -> validateStep5(state)
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
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    _uploadSuccess.value = false
                    onError("You must be logged in to create a property listing")
                    return@launch
                }

                Log.d(TAG, "=== Starting Property Submission ===")
                Log.d(TAG, "Current User ID: ${currentUser.uid}")
                Log.d(TAG, "Current User Email: ${currentUser.email}")

                _isUploading.value = true
                _uploadProgress.value = "Preparing images..."

                val allImageUris = mutableListOf<Uri>()
                state.imageCategories.forEach { category ->
                    category.images.forEach { uriString ->
                        try {
                            allImageUris.add(Uri.parse(uriString))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing URI: $uriString", e)
                        }
                    }
                }

                if (allImageUris.isEmpty()) {
                    _uploadProgress.value = "No images selected"
                    createAndSubmitProperty(state, emptyList(), currentUser.uid, onSuccess, onError)
                    return@launch
                }

                _uploadProgress.value = "Uploading ${allImageUris.size} images..."

                repository.uploadMultipleImages(context, allImageUris) { uploadedUrls ->
                    viewModelScope.launch {
                        if (uploadedUrls.isEmpty()) {
                            _isUploading.value = false
                            _uploadSuccess.value = false
                            onError("Failed to upload images. Please check your internet connection.")
                        } else {
                            _uploadProgress.value = "✅ ${uploadedUrls.size} images uploaded. Saving..."
                            createAndSubmitProperty(state, uploadedUrls, currentUser.uid, onSuccess, onError)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in submitListing", e)
                _isUploading.value = false
                _uploadSuccess.value = false
                _uploadProgress.value = ""
                onError("Error: ${e.message}")
            }
        }
    }

    private fun createAndSubmitProperty(
        state: PropertyListingState,
        uploadedUrls: List<String>,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = "Fetching owner information..."

                val database = FirebaseDatabase.getInstance()
                val userRef = database.getReference("users").child(currentUserId)

                val userSnapshot = userRef.get().await()

                val ownerName = userSnapshot.child("fullName").getValue(String::class.java)
                    ?: FirebaseAuth.getInstance().currentUser?.displayName
                    ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@")
                    ?: state.developer

                val ownerImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                val ownerEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

                Log.d(TAG, "=== Owner Information ===")
                Log.d(TAG, "Owner ID: $currentUserId")
                Log.d(TAG, "Owner Name: $ownerName")
                Log.d(TAG, "Owner Email: $ownerEmail")
                Log.d(TAG, "Owner Image: $ownerImageUrl")

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

                // ⭐ CRITICAL FIX: Log coordinate information before creating property
                Log.d(TAG, "=== Property Location Information ===")
                Log.d(TAG, "Location Selected: ${state.hasSelectedLocation}")
                Log.d(TAG, "Location Address: ${state.location}")
                Log.d(TAG, "Latitude: ${state.latitude}")
                Log.d(TAG, "Longitude: ${state.longitude}")

                // ⚠️ WARNING: Check if coordinates are default
                if (state.latitude == 27.7172 && state.longitude == 85.3240) {
                    Log.w(TAG, "⚠️ WARNING: Property is using default Kathmandu coordinates!")
                    Log.w(TAG, "⚠️ This property will NOT appear at its actual location on the map")

                    if (!state.hasSelectedLocation) {
                        Log.e(TAG, "❌ ERROR: Location was not selected on map!")
                        _isUploading.value = false
                        _uploadSuccess.value = false
                        onError("Please select the property location on the map")
                        return@launch
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
                    images = categorizedImages,
                    location = state.location,

                    // ⭐⭐⭐ CRITICAL FIX: Use actual coordinates from state ⭐⭐⭐
                    latitude = state.latitude,
                    longitude = state.longitude,

                    propertyType = state.selectedPropertyType,
                    marketType = state.selectedPurpose,
                    floor = state.floor,
                    furnishing = state.furnishing,
                    parking = state.parking,
                    petsAllowed = state.petsAllowed,
                    description = state.description,

                    // Owner information
                    ownerId = currentUserId,
                    ownerName = ownerName,
                    ownerImageUrl = ownerImageUrl,
                    ownerEmail = ownerEmail,

                    utilitiesIncluded = if (state.selectedPurpose != "Sell") state.utilitiesIncluded else null,
                    commission = if (state.selectedPurpose != "Sell") state.commission else null,
                    advancePayment = if (state.selectedPurpose != "Sell") state.advancePayment else null,
                    securityDeposit = if (state.selectedPurpose != "Sell") state.securityDeposit else null,
                    minimumLease = if (state.selectedPurpose != "Sell") state.minimumLease else null,
                    availableFrom = if (state.selectedPurpose != "Sell") state.availableFrom else null,

                    amenities = state.amenities,
                    status = PropertyStatus.PENDING,
                    isFavorite = false
                )

                // ⭐ FINAL VALIDATION LOG
                Log.d(TAG, "=== Final Property Object ===")
                Log.d(TAG, "Property ID: ${property.id}")
                Log.d(TAG, "Title: ${property.title}")
                Log.d(TAG, "Location: ${property.location}")
                Log.d(TAG, "Coordinates: (${property.latitude}, ${property.longitude})")
                Log.d(TAG, "Owner ID: ${property.ownerId}")

                _uploadProgress.value = "Saving property to database..."

                repository.addProperty(property) { success, error ->
                    _isUploading.value = false
                    if (success) {
                        _uploadSuccess.value = true
                        _uploadProgress.value = "✅ Property created successfully!"
                        Log.d(TAG, "✅ Property saved successfully!")
                        Log.d(TAG, "   ID: ${property.id}")
                        Log.d(TAG, "   Coordinates: (${property.latitude}, ${property.longitude})")
                        Log.d(TAG, "   Owner: ${property.ownerId}")
                        onSuccess()
                    } else {
                        _uploadSuccess.value = false
                        _uploadProgress.value = ""
                        Log.e(TAG, "❌ Failed to save property: $error")
                        onError(error ?: "Failed to save property")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in createAndSubmitProperty", e)
                _isUploading.value = false
                _uploadSuccess.value = false
                _uploadProgress.value = ""
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetUploadStatus() {
        _uploadSuccess.value = null
        _uploadProgress.value = ""
    }
}