package com.example.gharbato.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.GeminiChatMessage
import com.example.gharbato.repository.GeminiRepository
import com.example.gharbato.repository.GeminiRepositoryImpl
import kotlinx.coroutines.launch
import java.util.UUID

class GeminiChatViewModel(
    private val context: Context,
    private val userId: String
) : ViewModel() {

    private val repository: GeminiRepository = GeminiRepositoryImpl(context)

    private val _messages = mutableStateOf<List<GeminiChatMessage>>(emptyList())
    val messages: State<List<GeminiChatMessage>> = _messages

    private val _messageText = mutableStateOf("")
    val messageText: State<String> = _messageText

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _showWelcome = mutableStateOf(true)
    val showWelcome: State<Boolean> = _showWelcome

    init {
        loadConversation()
    }

    private fun loadConversation() {
        val savedMessages = repository.loadConversation(userId)
        _messages.value = savedMessages
        _showWelcome.value = savedMessages.isEmpty()
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty() || _isLoading.value) return

        _showWelcome.value = false

        // Add user message
        val userMessage = GeminiChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(userMessage)
        _messages.value = currentMessages
        _messageText.value = ""
        _isLoading.value = true

        // Save immediately
        saveConversation()

        // Send to Gemini
        viewModelScope.launch {
            repository.sendMessage(
                message = text,
                conversationHistory = currentMessages
            ) { success, response, propertyIds ->
                _isLoading.value = false

                val aiMessage = GeminiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = response,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    isError = !success,
                    propertyIds = propertyIds
                )

                val updatedMessages = _messages.value.toMutableList()
                updatedMessages.add(aiMessage)
                _messages.value = updatedMessages

                saveConversation()
            }
        }
    }

    fun clearConversation() {
        repository.clearConversation(userId)
        _messages.value = emptyList()
        _showWelcome.value = true
    }

    private fun saveConversation() {
        repository.saveConversation(userId, _messages.value)
    }

    fun sendQuickMessage(message: String) {
        _messageText.value = message
        sendMessage()
    }
}