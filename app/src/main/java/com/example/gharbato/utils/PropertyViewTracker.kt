package com.example.gharbato.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Calendar

private const val TAG = "PropertyViewTracker"

object PropertyViewTracker {

    /**
     * Track a property view
     * Updates: totalViews, todayViews, uniqueViewers, and viewerIds
     */
    fun trackPropertyView(propertyFirebaseKey: String, propertyId: Int) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val database = FirebaseDatabase.getInstance()
        val propertyRef = database.getReference("Property").child(propertyFirebaseKey)

        Log.d(TAG, "Tracking view for property: $propertyId by user: $currentUserId")

        // Use transaction to safely update view counts
        propertyRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                if (currentData.value == null) {
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                // Get current values
                val totalViews = currentData.child("totalViews").value as? Long ?: 0L
                val todayViews = currentData.child("todayViews").value as? Long ?: 0L
                val uniqueViewers = currentData.child("uniqueViewers").value as? Long ?: 0L
                val viewerIds = currentData.child("viewerIds").value as? Map<String, Long> ?: emptyMap()
                val lastViewedAt = currentData.child("lastViewedAt").value as? Long ?: 0L

                // Check if this is a new unique viewer
                val isNewViewer = !viewerIds.containsKey(currentUserId)

                // Check if we need to reset today's views (new day)
                val needsReset = !isSameDay(lastViewedAt, System.currentTimeMillis())

                // Update values
                currentData.child("totalViews").value = totalViews + 1
                currentData.child("todayViews").value = if (needsReset) 1L else todayViews + 1
                currentData.child("uniqueViewers").value = if (isNewViewer) uniqueViewers + 1 else uniqueViewers
                currentData.child("lastViewedAt").value = ServerValue.TIMESTAMP
                currentData.child("updatedAt").value = ServerValue.TIMESTAMP

                // Update viewer IDs map
                val updatedViewerIds = viewerIds.toMutableMap()
                updatedViewerIds[currentUserId] = System.currentTimeMillis()
                currentData.child("viewerIds").value = updatedViewerIds

                Log.d(TAG, "View tracked: totalViews=${totalViews + 1}, todayViews=${if (needsReset) 1 else todayViews + 1}, uniqueViewers=${if (isNewViewer) uniqueViewers + 1 else uniqueViewers}")

                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: com.google.firebase.database.DataSnapshot?
            ) {
                if (error != null) {
                    Log.e(TAG, "Failed to track view: ${error.message}")
                } else if (committed) {
                    Log.d(TAG, "View tracked successfully")
                }
            }
        })
    }

    /**
     * Track view using property ID (finds Firebase key first)
     */
    fun trackPropertyViewById(propertyId: Int) {
        val database = FirebaseDatabase.getInstance()
        val propertyRef = database.getReference("Property")

        propertyRef.orderByChild("id")
            .equalTo(propertyId.toDouble())
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val firebaseKey = snapshot.children.firstOrNull()?.key
                    if (firebaseKey != null) {
                        trackPropertyView(firebaseKey, propertyId)
                    } else {
                        Log.w(TAG, "Property not found with ID: $propertyId")
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e(TAG, "Failed to find property: ${error.message}")
                }
            })
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        if (timestamp1 == 0L) return false

        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Reset today's views at midnight (call this from a daily scheduler if needed)
     */
    fun resetTodayViews() {
        val database = FirebaseDatabase.getInstance()
        val propertyRef = database.getReference("Property")

        propertyRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                snapshot.children.forEach { propertySnapshot ->
                    val lastViewedAt = propertySnapshot.child("lastViewedAt").value as? Long ?: 0L

                    if (!isSameDay(lastViewedAt, System.currentTimeMillis())) {
                        propertySnapshot.ref.child("todayViews").setValue(0)
                        Log.d(TAG, "Reset today's views for property: ${propertySnapshot.key}")
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(TAG, "Failed to reset today's views: ${error.message}")
            }
        })
    }
}