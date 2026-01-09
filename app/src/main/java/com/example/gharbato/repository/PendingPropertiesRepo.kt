package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import kotlinx.coroutines.flow.Flow

interface PendingPropertiesRepo {
    fun getPendingProperties(): Flow<List<PropertyModel>>
    suspend fun approveProperty(propertyId: Int): Result<Boolean>
    suspend fun rejectProperty(propertyId: Int): Result<Boolean>
}