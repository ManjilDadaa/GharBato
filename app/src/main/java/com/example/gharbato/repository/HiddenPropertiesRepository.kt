package com.example.gharbato.repository

import kotlinx.coroutines.flow.Flow

interface HiddenPropertiesRepository {
    suspend fun getHiddenPropertyIds(): Set<Int>
    suspend fun hideProperty(propertyId: Int)
    suspend fun unhideProperty(propertyId: Int)
    suspend fun isPropertyHidden(propertyId: Int): Boolean
    fun getHiddenPropertyIdsFlow(): Flow<Set<Int>>
}
