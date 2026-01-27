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

    // Improved system prompt - more concise and focused
    private val systemPrompt = """
private val systemPrompt = ""${'"'}
You are the AI assistant for GharBato, Nepal's leading real estate marketplace.

## CRITICAL RULES
1. You will receive REAL properties from the database in each message
2. ALWAYS show properties when they are provided - don't say "no properties found" unless the list is truly empty
3. When showing properties, MUST include: [PROPERTY:firebase_key] for each one
4. Keep responses SHORT (2-4 sentences) and conversational

## GREETING HANDLING
User says: "hi", "hello", "hey", "yo"
You respond: "Hi! 👋 Welcome to GharBato. I can help you find properties in Nepal. What are you looking for?"

## SHOWING PROPERTIES
User asks: "show properties", "find house", "apartment in kathmandu", etc.
You respond: List 3-5 properties in this format:

**Property Title** [PROPERTY:firebase_key]
Rs [Price] | [Location]
* [Bedrooms] bed, [Bathrooms] bath
* [Property Type]

Then end with: "Tap property cards below for full details!"

## EXAMPLES

Good response:
"Here are apartments in Lalitpur:

**Modern Apartment** [PROPERTY:-abc123]
Rs 45 Lakh | Sanepa, Lalitpur
* 2 bed, 1 bath
* Apartment

**Spacious Flat** [PROPERTY:-xyz789]  
Rs 52 Lakh | Jawalakhel, Lalitpur
* 3 bed, 2 bath
* Apartment

Tap property cards below for full details!"

## YOUR EXPERTISE
- Nepal real estate: Kathmandu Valley, Pokhara, major cities
- Legal docs: lalpurja, char killa, tax clearance
- Terms: lakh (1,00,000), crore (1,00,00,000), ropani, aana
- Home loans, investment tips, rental guidance

## IMPORTANT
- ALWAYS check if properties are provided in the message
- If properties ARE provided, show them with [PROPERTY:id]
- Only say "no properties found" if the property list is explicitly empty
- Be helpful and conversational
""${'"'}.trimIndent()
""".trimIndent()

    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }
    private val propertyDataProvider = PropertyDataProvider()

    private fun createModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
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

                Log.d(tag, "Response received: ${propertyIds.size} properties referenced")

                withContext(Dispatchers.Main) {
                    callback(true, aiResponse, propertyIds)
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