package com.example.gharbato.model

data class NotificationModel(
    var notificationId: String = "",
    var userId: String = "",
    var title: String = "",
    var message: String = "",
    var type: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    var imageUrl: String = "",
    var actionData: String = ""
)