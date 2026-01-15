package com.example.gharbato.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PropertySummary(
    val id: String = "",
    val title: String = "",
    val price: String = "",
    val location: String = "",
    val propertyType: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val area: String = "",
    val purpose: String = "" // For Sale, For Rent
)

class PropertyDataProvider {
    private val database = FirebaseDatabase.getInstance()
    private val propertiesRef = database.getReference("properties")

    companion object {
        private const val TAG = "PropertyDataProvider"
    }

    /**
     * Fetch properties based on user query parameters
     */
    suspend fun searchProperties(
        purpose: String? = null, // "sale" or "rent"
        propertyType: String? = null, // "house", "apartment", "villa", etc.
        location: String? = null,
        maxResults: Int = 10
    ): List<PropertySummary> = suspendCancellableCoroutine { continuation ->

        propertiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val properties = mutableListOf<PropertySummary>()

                for (propertySnapshot in snapshot.children) {
                    try {
                        val id = propertySnapshot.key ?: continue
                        val title = propertySnapshot.child("title").getValue(String::class.java) ?: ""
                        val price = propertySnapshot.child("price").getValue(String::class.java) ?: ""
                        val propertyLocation = propertySnapshot.child("location").getValue(String::class.java) ?: ""
                        val type = propertySnapshot.child("propertyType").getValue(String::class.java) ?: ""
                        val bedrooms = propertySnapshot.child("bedrooms").getValue(Int::class.java) ?: 0
                        val bathrooms = propertySnapshot.child("bathrooms").getValue(Int::class.java) ?: 0
                        val area = propertySnapshot.child("area").getValue(String::class.java) ?: ""
                        val propertyPurpose = propertySnapshot.child("purpose").getValue(String::class.java) ?: ""

                        // Apply filters
                        var matches = true

                        if (purpose != null && !propertyPurpose.contains(purpose, ignoreCase = true)) {
                            matches = false
                        }

                        if (propertyType != null && !type.contains(propertyType, ignoreCase = true)) {
                            matches = false
                        }

                        if (location != null && !propertyLocation.contains(location, ignoreCase = true)) {
                            matches = false
                        }

                        if (matches) {
                            properties.add(
                                PropertySummary(
                                    id = id,
                                    title = title,
                                    price = price,
                                    location = propertyLocation,
                                    propertyType = type,
                                    bedrooms = bedrooms,
                                    bathrooms = bathrooms,
                                    area = area,
                                    purpose = propertyPurpose
                                )
                            )
                        }

                        if (properties.size >= maxResults) break

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing property: ${e.message}")
                    }
                }

                Log.d(TAG, "Found ${properties.size} properties")
                continuation.resume(properties)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                continuation.resume(emptyList())
            }
        })
    }

    /**
     * Get total count of properties
     */
    suspend fun getPropertyCount(): Int = suspendCancellableCoroutine { continuation ->
        propertiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume(0)
            }
        })
    }

    /**
     * Format properties for AI context
     */
    fun formatPropertiesForAI(properties: List<PropertySummary>): String {
        if (properties.isEmpty()) {
            return "No properties found matching the criteria."
        }

        return buildString {
            appendLine("Here are ${properties.size} properties from Gharbato:")
            appendLine()
            properties.forEachIndexed { index, property ->
                appendLine("${index + 1}. **${property.title}**")
                appendLine("   Price: ${property.price}")
                appendLine("   Location: ${property.location}")
                appendLine("   Type: ${property.propertyType}")
                appendLine("   ${property.bedrooms} bed, ${property.bathrooms} bath")
                if (property.area.isNotEmpty()) {
                    appendLine("   Area: ${property.area}")
                }
                appendLine()
            }
        }
    }
}