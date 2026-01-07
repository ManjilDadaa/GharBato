package com.example.gharbato.repository

import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.model.PropertyStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PendingPropertiesRepoImpl : PendingPropertiesRepo {
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
}