package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.model.PropertyStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PendingPropertiesRepoImpl : PendingPropertiesRepo {
    private val TAG = "PendingPropertiesRepo"

    private val database = FirebaseDatabase.getInstance()
    val ref = FirebaseDatabase.getInstance().getReference("Property")
    override suspend fun getPendingProperties(): List<PropertyModel> {
        return suspendCancellableCoroutine { continuation ->

            ref.orderByChild("status")
                .equalTo(PropertyStatus.PENDING)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val properties = mutableListOf<PropertyModel>()

                        for (propertySnapshot in snapshot.children) {
                            val property = propertySnapshot.getValue(PropertyModel::class.java)
                            if (property != null) {
                                properties.add(property)
                            }
                        }

                        continuation.resume(properties)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(
                            Exception(error.message)
                        )
                    }
                }
                )
        }
    }

    override suspend fun approveProperty(propertyId: Int): Result<Boolean> {
        return try {
            // Update the status to APPROVED
            val updates = hashMapOf<String, Any>(
                "status" to PropertyStatus.APPROVED
            )

            ref
                .orderByChild("id")
                .equalTo(propertyId.toDouble())
                .get()
                .await()
                .children
                .firstOrNull()
                ?.ref
                ?.updateChildren(updates)
                ?.await()

            Log.d(TAG, "Property $propertyId approved successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error approving property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun rejectProperty(propertyId: Int): Result<Boolean> {
        return try {
            // Update the status to REJECTED
            val updates = hashMapOf<String, Any>(
                "status" to PropertyStatus.REJECTED
            )
            ref
                .orderByChild("id")
                .equalTo(propertyId.toDouble())
                .get()
                .await()
                .children
                .firstOrNull()
                ?.ref
                ?.updateChildren(updates)
                ?.await()

            Log.d(TAG, "Property $propertyId rejected successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

}