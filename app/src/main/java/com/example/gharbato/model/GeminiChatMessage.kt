package com.example.gharbato.model

data class GeminiChatMessage(
    val id: String = "",
    val text: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val propertyIds: List<String> = emptyList()
)

data class GeminiConversation(
    val messages: List<GeminiChatMessage> = emptyList()
)