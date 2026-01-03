package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedPropertiesRepositoryImpl : SavedPropertiesRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // In-memory cache
    private val savedPropertiesMap = mutableMapOf<Int, PropertyModel>()
    private val _savedPropertiesFlow = MutableStateFlow<List<PropertyModel>>(emptyList())

    // Get current user ID (or use a default for testing)
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "default_user"
    }

    private fun getSavedPropertiesRef() =
        database.getReference("users/${getUserId()}/savedProperties")

    override suspend fun getSavedProperties(): List<PropertyModel> {
        return try {
            val snapshot = getSavedPropertiesRef().get().await()
            val properties = mutableListOf<PropertyModel>()

            snapshot.children.forEach { child ->
                child.getValue(PropertyModel::class.java)?.let { property ->
                    properties.add(property.copy(isFavorite = true))
                    savedPropertiesMap[property.id] = property.copy(isFavorite = true)
                }
            }

            _savedPropertiesFlow.value = properties
            properties
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveProperty(property: PropertyModel) {
        try {
            val propertyToSave = property.copy(isFavorite = true)

            // Save to Firebase
            getSavedPropertiesRef()
                .child(property.id.toString())
                .setValue(propertyToSave)
                .await()

            // Update in-memory cache
            savedPropertiesMap[property.id] = propertyToSave
            updateFlow()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeSavedProperty(propertyId: Int) {
        try {
            // Remove from Firebase
            getSavedPropertiesRef()
                .child(propertyId.toString())
                .removeValue()
                .await()

            // Remove from in-memory cache
            savedPropertiesMap.remove(propertyId)
            updateFlow()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeProperty(propertyId: Int) {
        removeSavedProperty(propertyId)
    }

    override suspend fun isPropertySaved(propertyId: Int): Boolean {
        return try {
            val snapshot = getSavedPropertiesRef()
                .child(propertyId.toString())
                .get()
                .await()

            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    override fun getSavedPropertiesFlow(): Flow<List<PropertyModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val properties = mutableListOf<PropertyModel>()
                savedPropertiesMap.clear()

                snapshot.children.forEach { child ->
                    child.getValue(PropertyModel::class.java)?.let { property ->
                        val savedProperty = property.copy(isFavorite = true)
                        properties.add(savedProperty)
                        savedPropertiesMap[property.id] = savedProperty
                    }
                }

                _savedPropertiesFlow.value = properties
                trySend(properties)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                close(error.toException())
            }
        }

        getSavedPropertiesRef().addValueEventListener(listener)

        awaitClose {
            getSavedPropertiesRef().removeEventListener(listener)
        }
    }

    private fun updateFlow() {
        _savedPropertiesFlow.value = savedPropertiesMap.values.toList()
    }
}