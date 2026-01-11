package com.example.gharbato.model

data class ChatSession(
    val chatId: String,
    val myUserId: String,
    val myUserName: String,
    val otherUserId: String
)
