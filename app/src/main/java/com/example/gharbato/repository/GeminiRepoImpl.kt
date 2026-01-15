package com.example.gharbato.repository

import android.content.Context
import android.util.Log
import com.example.gharbato.BuildConfig
import com.example.gharbato.model.GeminiChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class GeminiRepoImpl(
    private val context: Context
) : GeminiRepository {

    private val tag = "GeminiRepository"
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }

    // Initialize Gemini Model
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            },
            systemInstruction = Content.Builder().apply {
                text("""
                    You are a helpful AI assistant for a real estate application called "Gharbato".
                    Your role is to help users with:
                    - General real estate advice and information
                    - Property buying/selling guidance
                    - Rental market insights
                    - Home loan and mortgage information
                    - Property valuation basics
                    - Location and neighborhood information
                    - Real estate legal basics (general information only)
                    - Property investment tips
                    
                    Guidelines:
                    - Be friendly, professional, and concise
                    - Provide helpful, accurate information
                    - If you don't know something, admit it
                    - Don't provide specific legal or financial advice
                    - Encourage users to consult professionals for complex matters
                    - Keep responses focused and easy to understand
                    - Use simple language, avoid jargon when possible
                    
                    Remember: You're here to assist and guide, not to replace professional advice.
                """.trimIndent())
            }.build()
        )
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<GeminiChatMessage>,
        callback: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Sending message to Gemini: $message")

                // Build conversation history for context
                val chat = generativeModel.startChat(
                    history = conversationHistory
                        .filter { !it.isError } // Exclude error messages from history
                        .map { msg ->
                            Content.Builder().apply {
                                role = if (msg.isFromUser) "user" else "model"
                                text(msg.text)
                            }.build()
                        }
                )

                // Send message and get response
                val response = chat.sendMessage(message)
                val aiResponse = response.text ?: "I apologize, but I couldn't generate a response. Please try again."

                Log.d(tag, "Gemini response: $aiResponse")
                withContext(Dispatchers.Main) {
                    callback(true, aiResponse)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error sending message to Gemini", e)
                val errorMessage = when {
                    e.message?.contains("API key", ignoreCase = true) == true ->
                        "API key error. Please check your configuration."
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "API quota exceeded. Please try again later."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> "Sorry, I encountered an error: ${e.message ?: "Unknown error"}"
                }
                withContext(Dispatchers.Main) {
                    callback(false, errorMessage)
                }
            }
        }
    }

    override fun loadConversation(userId: String): List<GeminiChatMessage> {
        return try {
            val json = prefs.getString("conversation_$userId", null) ?: return emptyList()
            val type = object : TypeToken<List<GeminiChatMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error loading conversation", e)
            emptyList()
        }
    }

    override fun saveConversation(userId: String, messages: List<GeminiChatMessage>) {
        try {
            val json = gson.toJson(messages)
            prefs.edit().putString("conversation_$userId", json).apply()
            Log.d(tag, "Conversation saved successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error saving conversation", e)
        }
    }

    override fun clearConversation(userId: String) {
        prefs.edit().remove("conversation_$userId").apply()
        Log.d(tag, "Conversation cleared")
    }
}