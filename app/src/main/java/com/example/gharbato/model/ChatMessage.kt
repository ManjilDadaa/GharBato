package com.example.gharbato.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
