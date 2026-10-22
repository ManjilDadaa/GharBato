package com.example.gharbato.repository

import com.example.gharbato.model.PropertyModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedPropertiesRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : SavedPropertiesRepository {
    private fun savedRef(userId: String) =
        database.getReference("users/$userId/UserFavoriteList")

    private fun requireUserId(): String =
        checkNotNull(auth.currentUser?.uid) { "Sign in to save properties." }

    override suspend fun getSavedProperties(): List<PropertyModel> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return readProperties(savedRef(userId).get().await())
    }

    override suspend fun saveProperty(property: PropertyModel) {
        savedRef(requireUserId()).child(property.id.toString())
            .setValue(property.copy(isFavorite = true)).await()
    }

    override suspend fun removeSavedProperty(propertyId: Int) {
        savedRef(requireUserId()).child(propertyId.toString()).removeValue().await()
    }

    override suspend fun removeProperty(propertyId: Int) = removeSavedProperty(propertyId)

    override suspend fun isPropertySaved(propertyId: Int): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return savedRef(userId).child(propertyId.toString()).get().await().exists()
    }

    override fun getSavedPropertiesFlow(): Flow<List<PropertyModel>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        // Capture the exact reference so sign-out cannot detach from a different path.
        val reference = savedRef(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    trySend(readProperties(snapshot))
                } catch (e: Exception) {
                    close(e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    private fun readProperties(snapshot: DataSnapshot): List<PropertyModel> =
        snapshot.children.mapNotNull { it.getValue(PropertyModel::class.java)?.copy(isFavorite = true) }
}
