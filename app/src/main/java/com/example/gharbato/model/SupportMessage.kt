package com.example.gharbato.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SupportMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhone: String = "",
    val senderImage: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val admin: Boolean = false,  // Matches Firebase field name "admin"
    val isDelivered: Boolean = false,  // Message delivered to recipient's device
    val isRead: Boolean = false        // Message has been read/seen by recipient
) {
    // Check if message is from admin (excluded from Firebase serialization)
    @get:Exclude
    val isAdmin: Boolean
        get() = admin || senderId == "admin"
}