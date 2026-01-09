package com.example.gharbato.model

data class NotificationModel(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // e.g., "property", "message", "system", "update"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val imageUrl: String = "", // Optional image for notification
    val actionData: String = "" // Extra data for navigation (e.g., property ID)
)