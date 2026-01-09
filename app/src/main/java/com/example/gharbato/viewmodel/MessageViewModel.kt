package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.ChatMessage
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.ChatSession
import com.example.gharbato.repository.MessageRepository
import com.example.gharbato.repository.MessageRepositoryImpl

data class ChatNavigation(
    val targetUserId: String,
    val targetUserName: String,
    val targetUserImage: String
)

class MessageViewModel(
    private val repository: MessageRepository = MessageRepositoryImpl()
) : ViewModel() {

    // UI State
    private val _searchText = mutableStateOf("")
    val searchText: State<String> = _searchText

    private val _users = mutableStateOf<List<UserModel>>(emptyList())
    val users: State<List<UserModel>> = _users

    // Store all chat partners locally for filtering
    private val _allChatPartners = mutableStateOf<List<UserModel>>(emptyList())

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _currentUser = mutableStateOf<MessageUser?>(null)
    val currentUser: State<MessageUser?> = _currentUser

    private val _chatNavigation = mutableStateOf<ChatNavigation?>(null)
    val chatNavigation: State<ChatNavigation?> = _chatNavigation

    init {
        loadCurrentUser()
        loadUsers()
    }

    private fun loadCurrentUser() {
        _currentUser.value = repository.getCurrentUser()
    }

    fun loadUsers() {
        _isLoading.value = true
        repository.getChatPartners { success, userList, message ->
            _isLoading.value = false

            if (success && userList != null) {
                _allChatPartners.value = userList
                _users.value = userList
                if (userList.isEmpty()) {
                    _errorMessage.value = "No chat history found. Start a conversation to see users here."
                } else {
                    _errorMessage.value = ""
                }
            } else {
                _errorMessage.value = message ?: "Failed to load chat partners"
            }
        }
    }

    fun loadAllUsers() {
        _isLoading.value = true
        repository.getAllUsers { success, userList, message ->
            _isLoading.value = false

            if (success && userList != null) {
                _users.value = userList
                if (userList.isEmpty()) {
                    _errorMessage.value = "No registered users found in database"
                } else {
                    _errorMessage.value = ""
                }
            } else {
                _errorMessage.value = message ?: "Failed to load users"
            }
        }
    }

    fun onSearchTextChanged(newText: String) {
        _searchText.value = newText
        filterChatPartners()
    }

    private fun filterChatPartners() {
        val query = _searchText.value.trim().lowercase()
        if (query.isEmpty()) {
            _users.value = _allChatPartners.value
        } else {
            _users.value = _allChatPartners.value.filter { user ->
                user.fullName.lowercase().contains(query) ||
                        user.email.lowercase().contains(query) ||
                        user.userId.lowercase().contains(query)
            }
        }
    }

    fun searchUsers() {
        // This method now just calls the local filter
        filterChatPartners()
    }

    fun initiateCall(
        targetUserId: String,
        targetUserName: String,
        isVideoCall: Boolean,
        activity: android.app.Activity
    ) {
        repository.initiateCall(activity, targetUserId, targetUserName, isVideoCall)
    }

    fun navigateToChat(
        targetUserId: String,
        targetUserName: String,
        targetUserImage: String,
        activity: android.app.Activity
    ) {
        repository.navigateToChat(activity, targetUserId, targetUserName, targetUserImage)
    }

    fun requestChatNavigation(targetUserId: String, targetUserName: String, targetUserImage: String) {
        _chatNavigation.value = ChatNavigation(targetUserId, targetUserName, targetUserImage)
    }

    fun onChatNavigationHandled() {
        _chatNavigation.value = null
    }

    fun getLocalUserId(context: android.content.Context): String {
        return repository.getOrCreateLocalUserId(context)
    }
}

class MessageDetailsViewModel(
    private val repository: MessageRepository = MessageRepositoryImpl()
) : ViewModel() {

    private val _chatSession = mutableStateOf<ChatSession?>(null)
    val chatSession: State<ChatSession?> = _chatSession

    private val _messages = mutableStateOf<List<ChatMessage>>(emptyList())
    val messages: State<List<ChatMessage>> = _messages

    private val _messageText = mutableStateOf("")
    val messageText: State<String> = _messageText

    private var stopListening: (() -> Unit)? = null

    fun startChat(context: Context, otherUserId: String) {
        val existing = _chatSession.value
        if (existing != null && existing.otherUserId == otherUserId && stopListening != null) return

        stopListening?.invoke()
        val session = repository.createChatSession(context, otherUserId)
        _chatSession.value = session
        stopListening = repository.listenToChatMessages(
            chatId = session.chatId,
            onMessages = { _messages.value = it }
        )
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    fun sendTextMessage() {
        val session = _chatSession.value ?: return
        val text = _messageText.value
        if (text.isBlank()) return

        repository.sendTextMessage(
            chatId = session.chatId,
            senderId = session.myUserId,
            senderName = session.myUserName,
            text = text
        )
        _messageText.value = ""
    }

    fun sendImageMessage(context: Context, uri: Uri) {
        val session = _chatSession.value ?: return
        repository.sendImageMessage(
            context = context,
            chatId = session.chatId,
            senderId = session.myUserId,
            senderName = session.myUserName,
            imageUri = uri
        )
    }

    override fun onCleared() {
        stopListening?.invoke()
        stopListening = null
        super.onCleared()
    }
}
