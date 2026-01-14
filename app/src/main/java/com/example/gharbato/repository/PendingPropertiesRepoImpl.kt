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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class PendingPropertiesRepoImpl : PendingPropertiesRepo {
    private val TAG = "PendingPropertiesRepo"

    private val propertyRef = FirebaseDatabase.getInstance().getReference("Property")
    private val usersRef = FirebaseDatabase.getInstance().getReference("Users")

    override fun getPendingProperties(): Flow<List<PropertyModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val properties = snapshot.children.mapNotNull {
                    it.getValue(PropertyModel::class.java)
                }.filter { it.status == PropertyStatus.PENDING }

                Log.d(TAG, "Found ${properties.size} pending properties")

                // Launch coroutine to fetch owner names
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val propertiesWithOwners = fetchOwnerNamesForProperties(properties)
                        trySend(propertiesWithOwners)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching owner names: ${e.message}")
                        trySend(properties) // Send properties anyway
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
                close(error.toException())
            }
        }

        propertyRef.addValueEventListener(listener)
        awaitClose { propertyRef.removeEventListener(listener) }
    }

    private suspend fun fetchOwnerNamesForProperties(
        properties: List<PropertyModel>
    ): List<PropertyModel> = coroutineScope {
        if (properties.isEmpty()) {
            return@coroutineScope emptyList()
        }

        properties.map { property ->
            async {
                if (property.ownerId.isNotEmpty()) {
                    try {
                        val userSnapshot = usersRef.child(property.ownerId).get().await()

                        if (userSnapshot.exists()) {
                            // Debug: Print all available fields
                            Log.d(TAG, "User data for ${property.ownerId}: ${userSnapshot.value}")

                            val ownerName = userSnapshot.child("name").getValue(String::class.java)
                                ?: userSnapshot.child("userName").getValue(String::class.java)
                                ?: userSnapshot.child("fullName").getValue(String::class.java)
                                ?: "Unknown User"

                            val ownerEmail = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            val ownerImageUrl = userSnapshot.child("imageUrl").getValue(String::class.java)
                                ?: userSnapshot.child("profileImage").getValue(String::class.java)
                                ?: userSnapshot.child("profileImageUrl").getValue(String::class.java)
                                ?: ""

                            Log.d(TAG, "Fetched - Name: $ownerName, Email: $ownerEmail, ImageUrl: $ownerImageUrl for property ${property.id}")

                            property.copy(
                                ownerName = ownerName,
                                ownerEmail = ownerEmail,
                                ownerImageUrl = ownerImageUrl
                            )
                        } else {
                            Log.w(TAG, "User not found for ownerId: ${property.ownerId}")
                            property.copy(ownerName = "Unknown User")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching owner for property ${property.id}: ${e.message}")
                        property.copy(ownerName = "Unknown User")
                    }
                } else {
                    Log.w(TAG, "Property ${property.id} has empty ownerId")
                    property.copy(ownerName = "No Owner ID")
                }
            }
        }.awaitAll()
    }

    override suspend fun approveProperty(propertyId: Int): Result<Boolean> {
        return try {
            val propertyQuery = propertyRef.orderByChild("id").equalTo(propertyId.toDouble())
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
            val propertyQuery = propertyRef.orderByChild("id").equalTo(propertyId.toDouble())
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