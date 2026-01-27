package com.example.gharbato.model

data class SupportMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhone: String = "",
    val senderImage: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isAdmin: Boolean = false
)