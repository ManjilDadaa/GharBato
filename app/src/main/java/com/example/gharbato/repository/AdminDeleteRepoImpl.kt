package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
class AdminDeleteRepoImpl : AdminDeleteRepo {
    private val TAG = "AdminDeleteRepo"
    private val ref = FirebaseDatabase.getInstance().getReference("Property")

    override fun getRejectedProperties(): Flow<List<PropertyModel>>  = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val properties = snapshot.children.mapNotNull {
                    it.getValue(PropertyModel::class.java)
                }.filter { it.status == PropertyStatus.REJECTED }

                Log.d(TAG, "Loaded ${properties.size} rejected properties")
                trySend(properties)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read rejected properties", error.toException())
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun deleteProperty(propertyId: Int): Result<Boolean> {
        return  try {
            Log.d(TAG, "Attempting to delete property: $propertyId")

            val propertyQuery = ref.orderByChild("id").equalTo(propertyId.toDouble())
            val snapshot = propertyQuery.get().await()

            if (snapshot.exists()) {
                snapshot.children.forEach { dataSnapshot ->
                    // Permanently delete from Firebase
                    dataSnapshot.ref.removeValue().await()
                    Log.d(TAG, "Property $propertyId deleted permanently")
                }
                Result.success(true)
            } else {
                Log.w(TAG, "Property $propertyId not found")
                Result.failure(Exception("Property not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restoreProperty(propertyId: Int): Result<Boolean> {
        return try {
            Log.d(TAG, "Attempting to restore property: $propertyId")

            val propertyQuery = ref.orderByChild("id").equalTo(propertyId.toDouble())
            val snapshot = propertyQuery.get().await()

            if (snapshot.exists()) {
                snapshot.children.forEach { dataSnapshot ->
                    // Change status back to PENDING for review
                    dataSnapshot.ref.child("status").setValue(PropertyStatus.PENDING).await()
                    Log.d(TAG, "Property $propertyId restored to PENDING status")
                }
                Result.success(true)
            } else {
                Log.w(TAG, "Property $propertyId not found")
                Result.failure(Exception("Property not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

}