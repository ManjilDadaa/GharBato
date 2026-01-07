package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel

interface PendingPropertiesRepo {
    suspend fun getPendingProperties(): List<PropertyModel>
    suspend fun approveProperty(propertyId: Int): Result<Boolean>
    suspend fun rejectProperty(propertyId: Int): Result<Boolean>
}