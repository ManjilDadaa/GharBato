package com.example.gharbato.repository

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.viewmodel.DeletionRecord
import kotlinx.coroutines.flow.Flow

interface AdminDeleteRepo {
    fun getRejectedProperties(): Flow<List<PropertyModel>>
    fun getDeletionHistory(): Flow<List<DeletionRecord>>
    suspend fun deleteProperty(propertyId: Int, property: PropertyModel): Result<Boolean>
    suspend fun restoreProperty(propertyId: Int): Result<Boolean>

}