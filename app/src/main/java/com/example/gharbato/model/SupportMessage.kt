package com.example.gharbato.model

/**
 * Data model for support messages between users and admins
 */
data class SupportMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhone: String = "",
    val senderImage: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isAdmin: Boolean = false
)

/**
 * Data model for support conversation list items
 */
data class SupportConversation(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0
)