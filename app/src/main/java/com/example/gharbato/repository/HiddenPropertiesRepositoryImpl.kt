package com.example.gharbato.repository

import android.util.Log
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

private const val TAG = "HiddenPropertiesRepo"

class HiddenPropertiesRepositoryImpl : HiddenPropertiesRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // In-memory cache
    private val hiddenPropertyIds = mutableSetOf<Int>()
    private val _hiddenPropertyIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    private fun getHiddenPropertiesRef() =
        database.getReference("Users/${getUserId()}/HiddenProperties")

    override suspend fun getHiddenPropertyIds(): Set<Int> {
        val userId = getUserId()
        if (userId.isEmpty()) {
            Log.w(TAG, "No user logged in, returning empty hidden set")
            return emptySet()
        }

        return try {
            val snapshot = getHiddenPropertiesRef().get().await()
            val ids = mutableSetOf<Int>()

            snapshot.children.forEach { child ->
                child.key?.toIntOrNull()?.let { propertyId ->
                    ids.add(propertyId)
                }
            }

            hiddenPropertyIds.clear()
            hiddenPropertyIds.addAll(ids)
            _hiddenPropertyIdsFlow.value = ids.toSet()

            Log.d(TAG, "Loaded ${ids.size} hidden properties for user $userId")
            ids
        } catch (e: Exception) {
            Log.e(TAG, "Error loading hidden properties: ${e.message}")
            emptySet()
        }
    }

    override suspend fun hideProperty(propertyId: Int) {
        val userId = getUserId()
        if (userId.isEmpty()) {
            Log.w(TAG, "No user logged in, cannot hide property")
            return
        }

        try {
            // Save to Firebase with timestamp
            getHiddenPropertiesRef()
                .child(propertyId.toString())
                .setValue(System.currentTimeMillis())
                .await()

            // Update in-memory cache
            hiddenPropertyIds.add(propertyId)
            _hiddenPropertyIdsFlow.value = hiddenPropertyIds.toSet()

            Log.d(TAG, "Property $propertyId hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding property $propertyId: ${e.message}")
        }
    }

    override suspend fun unhideProperty(propertyId: Int) {
        val userId = getUserId()
        if (userId.isEmpty()) {
            Log.w(TAG, "No user logged in, cannot unhide property")
            return
        }

        try {
            getHiddenPropertiesRef()
                .child(propertyId.toString())
                .removeValue()
                .await()

            hiddenPropertyIds.remove(propertyId)
            _hiddenPropertyIdsFlow.value = hiddenPropertyIds.toSet()

            Log.d(TAG, "Property $propertyId unhidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unhiding property $propertyId: ${e.message}")
        }
    }

    override suspend fun isPropertyHidden(propertyId: Int): Boolean {
        val userId = getUserId()
        if (userId.isEmpty()) return false

        return try {
            val snapshot = getHiddenPropertiesRef()
                .child(propertyId.toString())
                .get()
                .await()

            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if property $propertyId is hidden: ${e.message}")
            false
        }
    }

    override fun getHiddenPropertyIdsFlow(): Flow<Set<Int>> = callbackFlow {
        val userId = getUserId()
        if (userId.isEmpty()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = mutableSetOf<Int>()
                hiddenPropertyIds.clear()

                snapshot.children.forEach { child ->
                    child.key?.toIntOrNull()?.let { propertyId ->
                        ids.add(propertyId)
                        hiddenPropertyIds.add(propertyId)
                    }
                }

                _hiddenPropertyIdsFlow.value = ids.toSet()
                trySend(ids.toSet())
                Log.d(TAG, "Hidden properties flow updated: ${ids.size} properties")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Hidden properties listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        getHiddenPropertiesRef().addValueEventListener(listener)

        awaitClose {
            getHiddenPropertiesRef().removeEventListener(listener)
        }
    }
}
