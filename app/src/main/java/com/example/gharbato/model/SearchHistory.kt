package com.example.gharbato.model

import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
data class SearchHistory(
    val id: String = "",
    val userId: String = "",
    val searchQuery: String = "",
    val searchType: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val locationAddress: String = "",
    val locationRadius: Float = 0f,
    val resultsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val filters: Map<String, String> = emptyMap()
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_LOCATION = "location"

        // Firebase paths
        const val COLLECTION_NAME = "search_history"

        // Field names for Firebase queries
        const val FIELD_USER_ID = "userId"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_SEARCH_QUERY = "searchQuery"
        const val FIELD_SEARCH_TYPE = "searchType"
    }


    fun getDisplayText(): String {
        return when (searchType) {
            TYPE_TEXT -> searchQuery
            TYPE_LOCATION -> locationAddress.ifBlank { "Location: ${locationLat}, ${locationLng}" }
            else -> searchQuery
        }
    }


    fun getDescription(): String {
        val parts = mutableListOf<String>()

        if (resultsCount > 0) {
            parts.add("$resultsCount results")
        }

        if (searchType == TYPE_LOCATION && locationRadius > 0) {
            parts.add("within ${locationRadius}km")
        }

        if (filters.isNotEmpty()) {
            parts.add("${filters.size} filters")
        }

        return parts.joinToString(" • ")
    }


    fun isLocationSearch(): Boolean = searchType == TYPE_LOCATION


    fun isTextSearch(): Boolean = searchType == TYPE_TEXT


    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> "${diff / 604800000}w ago"
        }
    }


    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "searchQuery" to searchQuery,
            "searchType" to searchType,
            "locationLat" to locationLat,
            "locationLng" to locationLng,
            "locationAddress" to locationAddress,
            "locationRadius" to locationRadius,
            "resultsCount" to resultsCount,
            "timestamp" to timestamp,
            "filters" to filters
        )
    }
}