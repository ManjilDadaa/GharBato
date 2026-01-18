package com.example.gharbato.repository

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyFilters
private const val TAG = "SearchFilterRepo"

interface SearchFilterRepo {
    suspend fun searchByText(properties: List<PropertyModel>, query: String): List<PropertyModel>
    suspend fun filterByLocation(properties: List<PropertyModel>, latitude: Double, longitude: Double, radiusKm: Float): List<PropertyModel>
    suspend fun applyFilters(properties: List<PropertyModel>, filters: PropertyFilters): List<PropertyModel>
    fun hasActiveFilters(filters: PropertyFilters): Boolean
}