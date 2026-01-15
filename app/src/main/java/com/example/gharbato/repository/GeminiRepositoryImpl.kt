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
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }

    // Initialize Gemini Model
    private fun createModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 20
                topP = 0.8f
                maxOutputTokens = 800
            },
            systemInstruction = content {
                text(
                    """
                    You are a helpful AI assistant for "Gharbato", a real estate app.
                    
                    YOUR ROLE:
                    Help users with property buying, selling, rentals, home loans, and real estate advice.
                    
                    RESPONSE STYLE - VERY IMPORTANT:
                    - Keep responses SHORT and CONCISE (2-4 sentences max for simple questions)
                    - Use bullet points sparingly (max 3-4 points)
                    - Be direct and friendly, not wordy
                    - Avoid lengthy explanations unless specifically asked
                    - Don't repeat the user's question back to them
                    - Get straight to the answer
                    
                    FORMATTING:
                    - Use **bold** for important terms (sparingly)
                    - Use numbered lists (1. 2. 3.) for step-by-step instructions
                    - Use bullet points (*) for short lists
                    - Keep each point brief (one line when possible)
                    
                    EXAMPLES OF GOOD RESPONSES:
                    Q: "What should I look for when buying a house?"
                    A: "Focus on these key factors:
                    1. Location and neighborhood safety
                    2. Property condition and age
                    3. Price vs market value
                    4. Future resale potential
                    
                    Would you like details on any specific aspect?"
                    
                    Q: "How to calculate EMI?"
                    A: "EMI = [P x R x (1+R)^N]/[(1+R)^N-1]
                    
                    Where P=loan amount, R=monthly interest rate, N=tenure in months.
                    
                    Most banks offer EMI calculators on their websites for easier calculation."
                    
                    GUIDELINES:
                    - Admit when you don't know something
                    - Don't give specific legal/financial advice
                    - Suggest consulting professionals for complex matters
                    - Stay focused on real estate topics
                    
                    Remember: Users prefer quick, helpful answers over long explanations!
                    """.trimIndent()
                )
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

                // Build conversation history for context
                val history = conversationHistory
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