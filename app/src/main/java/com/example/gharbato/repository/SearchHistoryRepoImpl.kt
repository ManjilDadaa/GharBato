package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.model.SearchHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class SearchHistoryRepoImpl : SearchHistoryRepo {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "SearchHistoryRepo"
        private const val MAX_HISTORY_ENTRIES = 50 // Keep only last 50 searches per user
    }

    /**
     * Get reference to user's search history in Firebase
     */
    private fun getUserHistoryRef(): DatabaseReference? {
        val userId = getCurrentUserId() ?: return null
        return database.getReference(SearchHistory.COLLECTION_NAME).child(userId)
    }

    override fun saveSearchHistory(
        searchHistory: SearchHistory,
        callback: (success: Boolean, message: String?) -> Unit
    ) {
        val historyRef = getUserHistoryRef()
        if (historyRef == null) {
            callback(false, "User not authenticated")
            return
        }

        try {
            // Generate ID if not provided
            val id = if (searchHistory.id.isBlank()) {
                historyRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                searchHistory.id
            }

            // Create entry with ID
            val entryWithId = searchHistory.copy(
                id = id,
                userId = getCurrentUserId() ?: ""
            )

            // Save to Firebase
            historyRef.child(id).setValue(entryWithId.toMap())
                .addOnSuccessListener {
                    Log.d(TAG, "Search history saved: $id")
                    callback(true, null)

                    // Clean up old entries if needed
                    cleanupOldEntries()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save search history", e)
                    callback(false, e.message)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving search history", e)
            callback(false, e.message)
        }
    }

    override fun getSearchHistory(
        limit: Int,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    ) {
        val historyRef = getUserHistoryRef()
        if (historyRef == null) {
            callback(false, null, "User not authenticated")
            return
        }

        try {
            historyRef
                .orderByChild(SearchHistory.FIELD_TIMESTAMP)
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val historyList = mutableListOf<SearchHistory>()

                        for (childSnapshot in snapshot.children) {
                            try {
                                val history = childSnapshot.getValue(SearchHistory::class.java)
                                if (history != null) {
                                    historyList.add(history)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing search history", e)
                            }
                        }

                        // Sort by timestamp descending (newest first)
                        historyList.sortByDescending { it.timestamp }

                        Log.d(TAG, "Retrieved ${historyList.size} search history entries")
                        callback(true, historyList, null)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to retrieve search history", error.toException())
                        callback(false, null, error.message)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving search history", e)
            callback(false, null, e.message)
        }
    }

    override fun getSearchHistoryByType(
        searchType: String,
        limit: Int,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    ) {
        val historyRef = getUserHistoryRef()
        if (historyRef == null) {
            callback(false, null, "User not authenticated")
            return
        }

        try {
            historyRef
                .orderByChild(SearchHistory.FIELD_SEARCH_TYPE)
                .equalTo(searchType)
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val historyList = mutableListOf<SearchHistory>()

                        for (childSnapshot in snapshot.children) {
                            try {
                                val history = childSnapshot.getValue(SearchHistory::class.java)
                                if (history != null) {
                                    historyList.add(history)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing search history", e)
                            }
                        }

                        // Sort by timestamp descending
                        historyList.sortByDescending { it.timestamp }

                        Log.d(TAG, "Retrieved ${historyList.size} search history entries of type: $searchType")
                        callback(true, historyList, null)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to retrieve search history by type", error.toException())
                        callback(false, null, error.message)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving search history by type", e)
            callback(false, null, e.message)
        }
    }

    override fun searchInHistory(
        query: String,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    ) {

        getSearchHistory(50) { success, history, message ->
            if (success && history != null) {
                val filtered = history.filter {
                    it.searchQuery.contains(query, ignoreCase = true) ||
                            it.locationAddress.contains(query, ignoreCase = true)
                }
                callback(true, filtered, null)
            } else {
                callback(false, null, message)
            }
        }
    }

    override fun deleteSearchHistory(
        historyId: String,
        callback: (success: Boolean, message: String?) -> Unit
    ) {
        val historyRef = getUserHistoryRef()
        if (historyRef == null) {
            callback(false, "User not authenticated")
            return
        }

        try {
            historyRef.child(historyId).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Search history deleted: $historyId")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete search history", e)
                    callback(false, e.message)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting search history", e)
            callback(false, e.message)
        }
    }

    override fun clearAllSearchHistory(
        callback: (success: Boolean, message: String?) -> Unit
    ) {
        val historyRef = getUserHistoryRef()
        if (historyRef == null) {
            callback(false, "User not authenticated")
            return
        }

        try {
            historyRef.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "All search history cleared")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to clear search history", e)
                    callback(false, e.message)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing search history", e)
            callback(false, e.message)
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    private fun cleanupOldEntries() {
        val historyRef = getUserHistoryRef() ?: return

        historyRef
            .orderByChild(SearchHistory.FIELD_TIMESTAMP)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount

                    if (count > MAX_HISTORY_ENTRIES) {
                        // Delete oldest entries
                        val toDelete = count - MAX_HISTORY_ENTRIES
                        var deleted = 0L

                        for (childSnapshot in snapshot.children) {
                            if (deleted >= toDelete) break

                            childSnapshot.ref.removeValue()
                            deleted++
                        }

                        Log.d(TAG, "Cleaned up $deleted old search history entries")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to cleanup old entries", error.toException())
                }
            })
    }
}