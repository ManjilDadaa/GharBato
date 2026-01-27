package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PropertyDataProvider {
    
    private val tag = "PropertyDataProvider"
    
    suspend fun fetchApprovedProperties(): List<PropertyModel> {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("Property")
                .get()
                .await()
            
            val properties = mutableListOf<PropertyModel>()
            snapshot.children.forEach { child ->
                child.getValue(PropertyModel::class.java)?.let { property ->
                    if (property.status == PropertyStatus.APPROVED) {
                        properties.add(property.copy(firebaseKey = child.key))
                    }
                }
            }
            
            Log.d(tag, "Fetched ${properties.size} approved properties")
            properties
        } catch (e: Exception) {
            Log.e(tag, "Error fetching properties", e)
            emptyList()
        }
    }
    
    fun formatPropertiesForAI(properties: List<PropertyModel>, query: String): String {
        if (properties.isEmpty()) {
            return "No properties available in the database currently."
        }
        
        val filtered = filterPropertiesByQuery(properties, query)
        
        if (filtered.isEmpty()) {
            return "No properties match the user's criteria. Suggest they browse all listings in the app."
        }
        
        val propertyList = filtered.take(5).joinToString("\n\n") { property ->
            """
            [PROPERTY:${property.firebaseKey}]
            Title: ${property.title}
            Price: ${property.price}
            Type: ${property.propertyType}
            Purpose: ${property.marketType}
            Location: ${property.location}
            Bedrooms: ${property.bedrooms}
            Bathrooms: ${property.bathrooms}
            Area: ${property.sqft}
            Status: ${property.propertyStatus}
            """.trimIndent()
        }
        
        return """
        AVAILABLE PROPERTIES IN GHARBATO DATABASE (${filtered.size} total, showing top 5):
        
        $propertyList
        
        IMPORTANT: 
        - Present these REAL properties to the user
        - Include [PROPERTY:firebase_key] in your response for each property
        - Format nicely with **bold** for titles and prices
        - Keep it concise - property cards will show below your message
        """.trimIndent()
    }
    
    private fun filterPropertiesByQuery(properties: List<PropertyModel>, query: String): List<PropertyModel> {
        val lowerQuery = query.lowercase()
        
        return properties.filter { property ->
            when {
                // Market type filters
                lowerQuery.contains("rent") && property.marketType.equals("Rent", ignoreCase = true) -> true
                lowerQuery.contains("buy") || lowerQuery.contains("sale") || lowerQuery.contains("sell") 
                    && property.marketType.equals("Sell", ignoreCase = true) -> true
                
                // Property type filters
                lowerQuery.contains("house") && property.propertyType.contains("House", ignoreCase = true) -> true
                lowerQuery.contains("apartment") && property.propertyType.contains("Apartment", ignoreCase = true) -> true
                lowerQuery.contains("land") && property.propertyType.contains("Land", ignoreCase = true) -> true
                
                // Location filters
                property.location.contains(lowerQuery, ignoreCase = true) -> true
                
                // Price filters
                lowerQuery.contains("cheap") || lowerQuery.contains("affordable") -> {
                    val price = property.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: Long.MAX_VALUE
                    price < 50_00_000 // Less than 50 lakhs
                }
                lowerQuery.contains("expensive") || lowerQuery.contains("luxury") -> {
                    val price = property.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0
                    price > 1_00_00_000 // More than 1 crore
                }
                
                // Bedroom filters
                lowerQuery.contains("1 bed") || lowerQuery.contains("1bed") -> property.bedrooms == 1
                lowerQuery.contains("2 bed") || lowerQuery.contains("2bed") -> property.bedrooms == 2
                lowerQuery.contains("3 bed") || lowerQuery.contains("3bed") -> property.bedrooms == 3
                lowerQuery.contains("4 bed") || lowerQuery.contains("4bed") -> property.bedrooms >= 4
                
                // General property query
                lowerQuery.contains("property") || lowerQuery.contains("properties") ||
                lowerQuery.contains("show") || lowerQuery.contains("find") ||
                lowerQuery.contains("available") -> true
                
                else -> false
            }
        }.sortedByDescending { it.createdAt }
    }
}
