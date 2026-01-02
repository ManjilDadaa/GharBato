package com.example.gharbato.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.view.DashboardActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ListingViewModel(private val repo : PropertyRepo) : ViewModel() {
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val ref: DatabaseReference = database.getReference("Listed_Properties")
    fun submitListing(context: Context, state: PropertyListingState){

        // 1. Collect all image URIs
        val imageUris = state.imageCategories
            .flatMap { it.images }
            .map { Uri.parse(it) }

        // 2. Upload images
        repo.uploadMultipleImages(context, imageUris) { uploadedUrls ->

            // 3. Convert ListingState → PropertyModel
            val property = PropertyModel(
                id = 0,
                title = state.title,
                price = state.price,
                location = state.location,
                bedrooms = state.bedrooms.toIntOrNull() ?: 0,
                bathrooms = state.bathrooms.toIntOrNull() ?: 0,
                imageUrl = uploadedUrls.firstOrNull() ?: "",
                propertyType = state.selectedPropertyType,
                developer = "",
                sqft = "",
                floor = "",
                furnishing = "",
                parking = false,
                petsAllowed = false,
                latLng = com.google.android.gms.maps.model.LatLng(0.0, 0.0)

            )

            // 4. Save to Firebase
            repo.addProperty(property){
                success, message ->
                if (success){
                    Toast.makeText(context, "Successfully listed", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}