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
    private val systemPrompt =
        """
        You are the AI assistant for "Gharbato", a real estate marketplace app in Nepal.

        CRITICAL INSTRUCTIONS:
        - When users ask about properties, you will receive REAL property data from the Gharbato database
        - ALWAYS use the actual property data provided to you
        - NEVER make up or invent property listings
        - When showing properties, mention their Property ID in format: [PROPERTY:id]
        - If no properties match, tell users honestly and suggest browsing the app

        RESPONSE STYLE:
        - Keep responses SHORT and conversational (2-4 sentences)
        - Be direct and helpful
        - Use **bold** for property titles and prices
        - Use bullet points (*) for property features
        - Always include [PROPERTY:id] when mentioning a property

        WHEN SHOWING PROPERTIES:
        Format each property like this:
        
        **Property Title** [PROPERTY:firebase_key]
        Rs Price | Location
        * Bedrooms bed, Bathrooms bath
        * Property Type
        
        EXAMPLES:

        User: "Show me houses for sale in Kathmandu"
        You: "Here are houses for sale in Kathmandu:
        
        **Modern Villa** [PROPERTY:-abc123]
        Rs 2.5 Cr | Kathmandu
        * 3 bed, 2 bath
        * House
        
        **Family Home** [PROPERTY:-xyz789]
        Rs 1.8 Cr | Kathmandu
        * 4 bed, 3 bath
        * House
        
        Tap any property card below to see full details!"

        User: "Find affordable apartments"
        You: "Here are affordable apartments:
        
        **Cozy Apartment** [PROPERTY:-def456]
        Rs 45 Lakh | Lalitpur
        * 2 bed, 1 bath
        * Apartment
        
        Check the property cards below for more info!"

        User: "What should I check when buying property?"
        You: "Key things to verify:
        * Legal documents and clear ownership
        * Property location and accessibility
        * Market price comparison
        * Future development plans in area
        
        Need help finding a specific property?"

        IMPORTANT:
        - Always include [PROPERTY:id] when mentioning properties
        - Keep responses concise
        - Property cards will appear automatically below your message
        - Guide users to tap cards for full details
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
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 20
                topP = 0.8f
                maxOutputTokens = 800
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
                Log.d(tag, "Sending message to Gemini: $message")

                // Fetch real properties from database
                val properties = propertyDataProvider.fetchApprovedProperties()
                val propertyContext = propertyDataProvider.formatPropertiesForAI(properties, message)

                val model = createModel()

                val history = mutableListOf(
                    content(role = "user") {
                        text(systemPrompt)
                    }
                )

                history += conversationHistory
                    .filter { !it.isError }
                    .takeLast(10)
                    .map { msg ->
                        content(role = if (msg.isFromUser) "user" else "model") {
                            text(msg.text)
                        }
                    }

                val chat = model.startChat(history = history)

                // Enhance message with property data
                val enhancedMessage = """$message
                    
                    $propertyContext
                """.trimIndent()

                // Send message and get response
                val response = chat.sendMessage(enhancedMessage)
                val aiResponse = response.text ?: "I apologize, but I couldn't generate a response. Please try again."

                // Extract property IDs from AI response
                val propertyIds = extractPropertyIds(aiResponse)

                Log.d(tag, "Gemini response received successfully with ${propertyIds.size} properties")
                withContext(Dispatchers.Main) {
                    callback(true, aiResponse, propertyIds)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error sending message to Gemini", e)
                Log.e(tag, "Error details: ${e.message}")
                Log.e(tag, "Error stacktrace: ", e)

                val errorMessage = when {
                    e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ->
                        "Invalid API key. Please check your Gemini API key."

                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "API quota exceeded. Please try again in a few minutes."

                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                        "Permission denied. Please check your API key permissions."

                    e.message?.contains("404") == true || e.message?.contains("NOT_FOUND") == true ->
                        "Model not available. Please try again."

                    e.message?.contains("429") == true || e.message?.contains("RESOURCE_EXHAUSTED") == true ->
                        "Too many requests. Please wait and try again."

                    e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."

                    e.message?.contains("DEADLINE_EXCEEDED", ignoreCase = true) == true ->
                        "Request timeout. Please try again."

                    else -> "Sorry, something went wrong. Please try again."
                }

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
            .toList()
            .take(5)
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
