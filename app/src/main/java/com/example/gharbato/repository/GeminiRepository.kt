package com.example.gharbato.repository

import com.example.gharbato.model.GeminiChatMessage

interface GeminiRepository {

    /**
     * Send a message to Gemini AI and get response
     * @param message User's message text
     * @param conversationHistory Previous messages for context
     * @param callback Returns success status and AI response or error message
     */
    suspend fun sendMessage(
        message: String,
        conversationHistory: List<GeminiChatMessage> = emptyList(),
        callback: (Boolean, String) -> Unit
    )

    /**
     * Load saved conversation from local storage
     * @param userId Current user's ID for storage key
     * @return List of saved messages
     */
    fun loadConversation(userId: String): List<GeminiChatMessage>

    /**
     * Save conversation to local storage
     * @param userId Current user's ID for storage key
     * @param messages List of messages to save
     */
    fun saveConversation(userId: String, messages: List<GeminiChatMessage>)

    /**
     * Clear conversation history
     * @param userId Current user's ID for storage key
     */
    fun clearConversation(userId: String)
}