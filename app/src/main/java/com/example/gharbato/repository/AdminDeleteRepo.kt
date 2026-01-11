package com.example.gharbato.repository

import com.example.gharbato.model.PropertyModel
import kotlinx.coroutines.flow.Flow

interface AdminDeleteRepo {
    fun getRejectedProperties(): Flow<List<PropertyModel>>
    suspend fun deleteProperty(propertyId: Int): Result<Boolean>
    suspend fun restoreProperty(propertyId: Int): Result<Boolean>

}