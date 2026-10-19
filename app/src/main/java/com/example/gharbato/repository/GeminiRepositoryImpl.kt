package com.example.gharbato.repository

import android.content.Context
import android.util.Log
import com.example.gharbato.model.GeminiChatMessage
import com.example.gharbato.utils.FirebaseFunctionsConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GeminiRepositoryImpl(
    private val context: Context
) : GeminiRepository {

    private val tag = "GeminiRepository"
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("gemini_chat_prefs", Context.MODE_PRIVATE)
    }
    private val propertyDataProvider = PropertyDataProvider()

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<GeminiChatMessage>,
        callback: (Boolean, String, List<String>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Sending message (${message.length} characters)")

                // Fetch approved property details locally; the Gemini key and prompt stay server-side.
                val properties = propertyDataProvider.fetchApprovedProperties()
                val propertyContext = propertyDataProvider.formatPropertiesForAI(properties, message)

                val history = conversationHistory
                    .filter { !it.isError }
                    .takeLast(20)
                    .map { msg ->
                        mapOf(
                            "role" to if (msg.isFromUser) "user" else "model",
                            "text" to msg.text.take(2_000)
                        )
                    }

                val result = Tasks.await(
                    FirebaseFunctions.getInstance(FirebaseFunctionsConfig.REGION)
                        .getHttpsCallable("askPropertyAssistant")
                        .call(
                            mapOf(
                                "message" to message.take(2_000),
                                "history" to history,
                                "propertyContext" to propertyContext.take(12_000)
                            )
                        ),
                    60,
                    TimeUnit.SECONDS
                )
                val responseData = result.data as? Map<*, *>
                val aiResponse = (responseData?.get("text") as? String)?.trim()
                    ?: "I apologize, I couldn't generate a response. Please try again."

                val propertyIds = extractPropertyIds(aiResponse)
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
            .distinct()
            .take(10)
            .toList()
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Configuration error. Please contact support."
            message.contains("quota", ignoreCase = true) ||
                message.contains("429") ||
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
            messages.takeLast(50)
        } catch (e: Exception) {
            Log.e(tag, "Error loading conversation", e)
            emptyList()
        }
    }

    override fun saveConversation(userId: String, messages: List<GeminiChatMessage>) {
        try {
            val messagesToSave = messages.takeLast(50)
            prefs.edit().putString("conversation_$userId", gson.toJson(messagesToSave)).apply()
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
