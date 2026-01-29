package com.example.gharbato.repository

import android.content.Context
import android.util.Log
import com.example.gharbato.BuildConfig
import com.example.gharbato.model.GeminiChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepositoryImpl(
    private val context: Context
) : GeminiRepository {

    private val tag = "GeminiRepository"

    private val systemPrompt = """
You are the AI assistant for GharBato, Nepal's real estate marketplace.

## RULES
1. Keep responses SHORT (1-3 sentences)
2. When showing properties, include [PROPERTY:firebase_key] but NEVER show the ID to users
3. Ask clarifying questions when needed (property type, location, budget, bedrooms)
4. Show 2-4 properties maximum per response

## GREETING
User: "hi", "hello"
You: "Hi! 👋 Looking for a property? Tell me your preferred location and budget."

## VAGUE REQUESTS
User: "show properties", "find house"
You: "I can help! What's your preferred location? (e.g., Kathmandu, Lalitpur, Pokhara)"

User: "apartment in kathmandu"
You: "Great! What's your budget range? How many bedrooms do you need?"

## SHOWING PROPERTIES
When user provides details, show properties:

"Here are [number] options in [location]:

[PROPERTY:firebase_key]
[PROPERTY:firebase_key]

Check the cards below for details!"

DO NOT write property details in text - only include [PROPERTY:id] tags. The cards will show everything.

## EXAMPLE
User: "2 bedroom apartment in Lalitpur under 50 lakh"
You: "Found 3 apartments in Lalitpur within your budget:

[PROPERTY:-abc123]
[PROPERTY:-xyz789]
[PROPERTY:-def456]

Tap the cards below to view details!"

## EXPERTISE
- Nepal real estate terms: lakh, crore, ropani, aana
- Legal docs: lalpurja, char killa
- Home loans, investment advice
""".trimIndent()

    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }
    private val propertyDataProvider = PropertyDataProvider()

    private fun createModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(systemPrompt) }, // Using systemInstruction
            generationConfig = generationConfig {
                temperature = 0.7f // Slightly higher for more natural responses
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024 // Increased for property listings
            }
        )
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<GeminiChatMessage>,
        callback: (Boolean, String, List<String>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Sending message: $message")

                // Fetch real properties from database
                val properties = propertyDataProvider.fetchApprovedProperties()
                val propertyContext = propertyDataProvider.formatPropertiesForAI(properties, message)

                val model = createModel()

                // Build conversation history (exclude system prompt since it's in systemInstruction)
                val history = conversationHistory
                    .filter { !it.isError }
                    .takeLast(20) // Increased to maintain better context
                    .map { msg ->
                        content(role = if (msg.isFromUser) "user" else "model") {
                            text(msg.text)
                        }
                    }

                val chat = model.startChat(history = history)

                // Build enhanced message with property context
                val enhancedMessage = buildString {
                    append(message)
                    if (propertyContext.isNotBlank()) {
                        append("\n\n---AVAILABLE PROPERTIES---\n")
                        append(propertyContext)
                        append("\n---END PROPERTIES---")
                    }
                }

                Log.d(tag, "Enhanced message length: ${enhancedMessage.length}")

                // Send message and get response
                val response = chat.sendMessage(enhancedMessage)
                val aiResponse = response.text?.trim()
                    ?: "I apologize, I couldn't generate a response. Please try again."

                // Extract property IDs from AI response
                val propertyIds = extractPropertyIds(aiResponse)
                
                // Remove property ID tags from user-facing response
                val cleanResponse = aiResponse.replace(Regex("\\[PROPERTY:[^\\]]+\\]"), "").trim()

                Log.d(tag, "Response received: ${propertyIds.size} properties referenced")

                withContext(Dispatchers.Main) {
                    callback(true, cleanResponse, propertyIds)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error sending message", e)

                val errorMessage = getErrorMessage(e)

                withContext(Dispatchers.Main) {
                    callback(false, errorMessage, emptyList())
                }
            }
        }
    }

    private fun extractPropertyIds(response: String): List<String> {
        val regex = """\[PROPERTY:([^\]]+)\]""".toRegex()
        return regex.findAll(response)
            .map { it.groupValues[1].trim() }
            .distinct() // Remove duplicates
            .take(10) // Support up to 10 properties
            .toList()
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Configuration error. Please contact support."

            message.contains("quota", ignoreCase = true) ||
                    message.contains("429") == true ||
                    message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "Service is busy. Please try again in a moment."

            message.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Service access error. Please contact support."

            message.contains("404", ignoreCase = true) ||
                    message.contains("NOT_FOUND", ignoreCase = true) ->
                "Service temporarily unavailable. Please try again."

            message.contains("network", ignoreCase = true) ||
                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("UnknownHost", ignoreCase = true) ->
                "No internet connection. Please check your network."

            message.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ->
                "Request timed out. Please try again."

            message.contains("INVALID_ARGUMENT", ignoreCase = true) ->
                "Invalid request. Please rephrase your question."

            else -> "Something went wrong. Please try again."
        }
    }

    override fun loadConversation(userId: String): List<GeminiChatMessage> {
        return try {
            val json = prefs.getString("conversation_$userId", null) ?: return emptyList()
            val type = object : TypeToken<List<GeminiChatMessage>>() {}.type
            val messages: List<GeminiChatMessage> = gson.fromJson(json, type) ?: emptyList()

            // Limit stored messages to prevent memory issues
            messages.takeLast(50)
        } catch (e: Exception) {
            Log.e(tag, "Error loading conversation", e)
            emptyList()
        }
    }

    override fun saveConversation(userId: String, messages: List<GeminiChatMessage>) {
        try {
            // Only save last 50 messages to prevent storage bloat
            val messagesToSave = messages.takeLast(50)
            val json = gson.toJson(messagesToSave)
            prefs.edit().putString("conversation_$userId", json).apply()
            Log.d(tag, "Conversation saved: ${messagesToSave.size} messages")
        } catch (e: Exception) {
            Log.e(tag, "Error saving conversation", e)
        }
    }

    override fun clearConversation(userId: String) {
        try {
            prefs.edit().remove("conversation_$userId").apply()
            Log.d(tag, "Conversation cleared for user: $userId")
        } catch (e: Exception) {
            Log.e(tag, "Error clearing conversation", e)
        }
    }
}