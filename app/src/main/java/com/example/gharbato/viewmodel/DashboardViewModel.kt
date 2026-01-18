package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.repository.MessageRepository
import com.example.gharbato.repository.MessageRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val repository: MessageRepository = MessageRepositoryImpl()
) : ViewModel() {

    data class IncomingMessagePreview(
        val id: String,
        val senderName: String,
        val text: String,
        val timestamp: Long
    )

    private val _unreadMessageCount = MutableStateFlow(0)
    val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()

    private val _latestIncomingMessage = MutableStateFlow<IncomingMessagePreview?>(null)
    val latestIncomingMessage: StateFlow<IncomingMessagePreview?> = _latestIncomingMessage.asStateFlow()

    private var stopListening: (() -> Unit)? = null

    init {
        startListeningToUnreadCount()
    }

    private fun startListeningToUnreadCount() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            stopListening = repository.listenToTotalUnreadCount(currentUser.uid) { count, latestMessage ->
                _unreadMessageCount.value = count
                if (latestMessage != null) {
                    _latestIncomingMessage.value = IncomingMessagePreview(
                        id = latestMessage.id,
                        senderName = latestMessage.senderName,
                        text = if (latestMessage.text.isNotBlank()) latestMessage.text else if (latestMessage.imageUrl.isNotBlank()) "Photo" else "",
                        timestamp = latestMessage.timestamp
                    )
                }
            }
        }
    }

    override fun onCleared() {
        stopListening?.invoke()
        super.onCleared()
    }
}

class DashboardViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
