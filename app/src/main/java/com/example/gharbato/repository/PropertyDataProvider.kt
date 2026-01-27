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
                    if (property.status == PropertyStatus.APPROVED && 
                        property.propertyStatus == "AVAILABLE") {
                        properties.add(property.copy(firebaseKey = child.key))
                    }
                }
            }

            Log.d(tag, "Fetched ${properties.size} approved and available properties")
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

        Log.d(tag, "Query: $query")
        Log.d(tag, "Total properties: ${properties.size}")
        Log.d(tag, "Filtered properties: ${filtered.size}")

        // If filtering removes everything, return all properties instead
        val propertiesToShow = if (filtered.isEmpty()) {
            Log.d(tag, "Filter too strict, showing all properties")
            properties
        } else {
            filtered
        }

        val propertyList = propertiesToShow.take(10).joinToString("\n\n") { property ->
            """
            [PROPERTY:${property.firebaseKey}]
            Title: ${property.title}
            Price: ${property.price}
            Type: ${property.propertyType}
            Purpose: ${property.marketType}
            Location: ${property.location}
            Bedrooms: ${property.bedrooms}
            Bathrooms: ${property.bathrooms}
            Area: ${property.sqft} sq.ft
            Status: ${property.propertyStatus}
            """.trimIndent()
        }

        return """
        AVAILABLE PROPERTIES IN GHARBATO DATABASE (${propertiesToShow.size} total, showing top ${propertiesToShow.take(10).size}):
        
        $propertyList
        
        INSTRUCTIONS FOR AI:
        - These are REAL properties from the database
        - MUST include [PROPERTY:firebase_key] when mentioning any property
        - Format each property nicely with **bold** titles and prices
        - Keep response concise (2-4 sentences)
        - End with "Tap property cards below for full details!"
        """.trimIndent()
    }

    private fun filterPropertiesByQuery(properties: List<PropertyModel>, query: String): List<PropertyModel> {
        val lowerQuery = query.lowercase()

        // For very short or generic queries, return all properties
        if (lowerQuery.length < 3 ||
            lowerQuery in listOf("hi", "hello", "hey", "yo", "show", "find", "search", "properties")) {
            return properties.sortedByDescending { it.createdAt }
        }

        // Apply filters - properties matching ANY criteria will be included
        val matchedProperties = properties.filter { property ->
            // Market type filters
            (lowerQuery.contains("rent") && property.marketType.equals("Rent", ignoreCase = true)) ||
                    ((lowerQuery.contains("buy") || lowerQuery.contains("sale") || lowerQuery.contains("sell"))
                            && property.marketType.equals("Sell", ignoreCase = true)) ||

                    // Property type filters
                    (lowerQuery.contains("house") && property.propertyType.contains("House", ignoreCase = true)) ||
                    (lowerQuery.contains("apartment") && property.propertyType.contains("Apartment", ignoreCase = true)) ||
                    (lowerQuery.contains("land") && property.propertyType.contains("Land", ignoreCase = true)) ||

                    // Location filters - check all major locations
                    property.location.contains("kathmandu", ignoreCase = true) && lowerQuery.contains("kathmandu") ||
                    property.location.contains("lalitpur", ignoreCase = true) && lowerQuery.contains("lalitpur") ||
                    property.location.contains("bhaktapur", ignoreCase = true) && lowerQuery.contains("bhaktapur") ||
                    property.location.contains("pokhara", ignoreCase = true) && lowerQuery.contains("pokhara") ||
                    property.location.contains(lowerQuery, ignoreCase = true) ||

                    // Price filters
                    (lowerQuery.contains("cheap") || lowerQuery.contains("affordable")) && run {
                val price = property.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: Long.MAX_VALUE
                price < 50_00_000 // Less than 50 lakhs
            } ||
                    (lowerQuery.contains("expensive") || lowerQuery.contains("luxury")) && run {
                val price = property.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0
                price > 1_00_00_000 // More than 1 crore
            } ||

                    // Bedroom filters
                    (lowerQuery.contains("1 bed") || lowerQuery.contains("1bed") || lowerQuery.contains("1 bhk")) && property.bedrooms == 1 ||
                    (lowerQuery.contains("2 bed") || lowerQuery.contains("2bed") || lowerQuery.contains("2 bhk")) && property.bedrooms == 2 ||
                    (lowerQuery.contains("3 bed") || lowerQuery.contains("3bed") || lowerQuery.contains("3 bhk")) && property.bedrooms == 3 ||
                    (lowerQuery.contains("4 bed") || lowerQuery.contains("4bed") || lowerQuery.contains("4 bhk")) && property.bedrooms >= 4 ||

                    // Title or description match
                    property.title.contains(lowerQuery, ignoreCase = true)
        }

        return matchedProperties.sortedByDescending { it.createdAt }
    }
}