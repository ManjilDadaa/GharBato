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
        - If no properties match, tell users honestly
        - Guide users to use the app's Buy/Rent sections to browse all listings

        RESPONSE STYLE:
        - Keep responses SHORT (2-4 sentences for simple questions)
        - Be direct and helpful
        - Use bullet points sparingly (max 3-4 items)
        - Format important info with **bold**
        - Use numbered lists for steps

        WHEN SHOWING PROPERTIES:
        - Present the actual properties from the database
        - Include key details: price, location, bedrooms/bathrooms
        - Keep descriptions brief
        - Suggest users tap on listings in the app for full details

        EXAMPLES:

        Q: "Show me houses for sale"
        A: [You'll receive real property data]
        "Here are available houses for sale:

        1. **Modern Villa in Kathmandu**
           Rs. 2.5 Cr | 3 bed, 2 bath | Kathmandu

        2. **Family House in Lalitpur**
           Rs. 1.8 Cr | 4 bed, 3 bath | Lalitpur

        Tap any listing in the app to see photos and contact the seller!"

        Q: "What should I check when buying property?"
        A: "Key things to verify:
        1. Legal documents and ownership
        2. Property location and accessibility
        3. Market price comparison
        4. Future development plans in area

        Need help with anything specific?"

        GENERAL ADVICE:
        - Help with real estate questions
        - Explain buying/renting process
        - Discuss home loans and EMI
        - Give property investment tips
        - Don't give specific legal/financial advice

        Remember: You have access to REAL Gharbato property data - use it!
        """.trimIndent()
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }
    private val propertyDataProvider = PropertyDataProvider()

    private fun createModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
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
        callback: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Sending message to Gemini: $message")

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

                // Send message and get response
                val response = chat.sendMessage(message)
                val aiResponse = response.text ?: "I apologize, but I couldn't generate a response. Please try again."

                Log.d(tag, "Gemini response received successfully")
                withContext(Dispatchers.Main) {
                    callback(true, aiResponse)
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
