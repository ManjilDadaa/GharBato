package com.example.gharbato.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.MessageRepository
import com.example.gharbato.repository.MessageRepositoryImpl
import kotlinx.coroutines.delay

class MessageViewModel(
    private val repository: MessageRepository = MessageRepositoryImpl()
) : ViewModel() {
    
    // UI State
    private val _searchText = mutableStateOf("")
    val searchText: State<String> = _searchText
    
    private val _users = mutableStateOf<List<UserModel>>(emptyList())
    val users: State<List<UserModel>> = _users
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    private val _currentUser = mutableStateOf<MessageUser?>(null)
    val currentUser: State<MessageUser?> = _currentUser
    
    init {
        loadCurrentUser()
        loadUsers()
    }
    
    private fun loadCurrentUser() {
        _currentUser.value = repository.getCurrentUser()
    }
    
    fun loadUsers() {
        _isLoading.value = true
        repository.getAllUsers { success, userList, message ->
            _isLoading.value = false
            
            if (success && userList != null) {
                _users.value = userList
                if (userList.isEmpty()) {
                    _errorMessage.value = "No registered users found in database"
                } else {
                    _errorMessage.value = "Found ${userList.size} registered users"
                }
            } else {
                _users.value = emptyList()
                _errorMessage.value = "Error loading users: $message"
            }
        }
    }
    
    fun onSearchTextChanged(newText: String) {
        _searchText.value = newText
    }
    
    fun searchUsers() {
        if (_isLoading.value) return
        
        try {
            repository.searchUsers(_searchText.value) { success, userList, message ->
                if (success && userList != null) {
                    _users.value = userList
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = message
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Search timeout"
        }
    }
    
    fun initiateCall(targetUserId: String, targetUserName: String, isVideoCall: Boolean, activity: android.app.Activity) {
        repository.initiateCall(activity, targetUserId, targetUserName, isVideoCall)
    }
    
    fun navigateToChat(targetUserId: String, targetUserName: String, activity: android.app.Activity) {
        repository.navigateToChat(activity, targetUserId, targetUserName)
    }
    
    fun getLocalUserId(context: android.content.Context): String {
        return repository.getOrCreateLocalUserId(context)
    }
}
