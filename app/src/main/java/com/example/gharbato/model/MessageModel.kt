package com.example.gharbato.model

data class MessageUser(
    val userId: String = "",
    val userName: String = "",
    val displayName: String = "",
    val lastMessage: String = "Hello, how are you?",
    val isOnline: Boolean = false
)

data class CallRequest(
    val callId: String = "",
    val userId: String = "",
    val userName: String = "",
    val targetUserId: String = "",
    val targetUserName: String = "",
    val isVideoCall: Boolean = false,
    val isIncomingCall: Boolean = false
)
