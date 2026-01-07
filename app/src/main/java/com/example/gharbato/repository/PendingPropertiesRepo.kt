package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel

interface PendingPropertiesRepo {
    suspend fun getPendingProperties(): List<PropertyModel>
}