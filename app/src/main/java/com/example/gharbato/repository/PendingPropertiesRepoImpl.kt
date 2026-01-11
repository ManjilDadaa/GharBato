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

class PendingPropertiesRepoImpl : PendingPropertiesRepo {
    private val TAG = "PendingPropertiesRepo"

    private val ref = FirebaseDatabase.getInstance().getReference("Property")

    override fun getPendingProperties(): Flow<List<PropertyModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val properties = snapshot.children.mapNotNull {
                    it.getValue(PropertyModel::class.java)
                }.filter { it.status == PropertyStatus.PENDING }
                trySend(properties)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun approveProperty(propertyId: Int): Result<Boolean> {
        return try {
            val propertyQuery = ref.orderByChild("id").equalTo(propertyId.toDouble())
            val snapshot = propertyQuery.get().await()

            if (snapshot.exists()) {
                snapshot.children.forEach { dataSnapshot ->
                    dataSnapshot.ref.child("status").setValue(PropertyStatus.APPROVED).await()
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Property not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error approving property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun rejectProperty(propertyId: Int): Result<Boolean> {
        return try {
            val propertyQuery = ref.orderByChild("id").equalTo(propertyId.toDouble())
            val snapshot = propertyQuery.get().await()

            if (snapshot.exists()) {
                snapshot.children.forEach { dataSnapshot ->
                    dataSnapshot.ref.child("status").setValue(PropertyStatus.REJECTED).await()
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Property not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }
}