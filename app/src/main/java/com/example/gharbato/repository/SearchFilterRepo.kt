package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.model.PropertyFilters

interface SearchFilterRepo {

    suspend fun searchByText(
        properties: List<PropertyModel>,
        query: String
    ): List<PropertyModel>

    suspend fun filterByLocation(
        properties: List<PropertyModel>,
        latitude: Double,
        longitude: Double,
        radiusKm: Float
    ): List<PropertyModel>

    suspend fun applyFilters(
        properties: List<PropertyModel>,
        filters: PropertyFilters
    ): List<PropertyModel>

    fun hasActiveFilters(filters: PropertyFilters): Boolean

    fun extractPriceValue(priceString: String): Int

    fun extractAreaValue(areaString: String): Int
}