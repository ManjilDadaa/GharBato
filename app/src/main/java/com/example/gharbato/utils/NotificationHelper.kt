package com.example.gharbato.utils

import android.content.Context
import android.content.Intent
import com.example.gharbato.model.ChatMessage
import com.example.gharbato.view.FloatingCallActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object NotificationHelper {

    private var isListeningToMessages = false
    private val lastMessageTimestamps = mutableMapOf<String, Long>()
    private val initializedChats = mutableSetOf<String>()

    fun showIncomingCallFloatingUI(
        context: Context,
        callerName: String,
        isVideoCall: Boolean,
        callId: String,
        currentUserId: String,
        callerId: String
    ) {
        val intent = Intent(context, FloatingCallActivity::class.java).apply {
            putExtra(FloatingCallActivity.EXTRA_TYPE, FloatingCallActivity.TYPE_CALL)
            putExtra(FloatingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(FloatingCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(FloatingCallActivity.EXTRA_IS_VIDEO_CALL, isVideoCall)
            putExtra(FloatingCallActivity.EXTRA_CURRENT_USER_ID, currentUserId)
            putExtra(FloatingCallActivity.EXTRA_OTHER_USER_ID, callerId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    fun showIncomingMessageFloatingUI(
        context: Context,
        chatId: String,
        senderId: String,
        senderName: String,
        messageText: String
    ) {
        val intent = Intent(context, FloatingCallActivity::class.java).apply {
            putExtra(FloatingCallActivity.EXTRA_TYPE, FloatingCallActivity.TYPE_MESSAGE)
            putExtra(FloatingCallActivity.EXTRA_CHAT_ID, chatId)
            putExtra(FloatingCallActivity.EXTRA_SENDER_ID, senderId)
            putExtra(FloatingCallActivity.EXTRA_SENDER_NAME, senderName)
            putExtra(FloatingCallActivity.EXTRA_MESSAGE_TEXT, messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    fun startListeningForMessages(context: Context) {
        if (isListeningToMessages) return

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val chatsRef = database.getReference("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    if (!chatId.contains(currentUserId)) continue

                    val messagesSnapshot = chatSnapshot.child("messages")
                    var latestMessage: ChatMessage? = null

                    for (msgSnapshot in messagesSnapshot.children) {
                        val msg = try {
                            msgSnapshot.getValue(ChatMessage::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        if (msg != null && msg.senderId != currentUserId) {
                            if (latestMessage == null || msg.timestamp > latestMessage!!.timestamp) {
                                latestMessage = msg
                            }
                        }
                    }

                    if (latestMessage != null) {
                        val existingTimestamp = lastMessageTimestamps[chatId]
                        if (existingTimestamp == null) {
                            lastMessageTimestamps[chatId] = latestMessage.timestamp
                            initializedChats.add(chatId)
                        } else if (latestMessage.timestamp > existingTimestamp && initializedChats.contains(chatId)) {
                            lastMessageTimestamps[chatId] = latestMessage.timestamp

                            val userIds = chatId.split("_")
                            val otherUserId = userIds.firstOrNull { it != currentUserId } ?: continue
                            val senderName = latestMessage.senderName.ifBlank { "New message" }
                            val text = when {
                                latestMessage.text.isNotBlank() -> latestMessage.text
                                latestMessage.imageUrl.isNotBlank() -> "Photo"
                                latestMessage.hasPropertyCard -> latestMessage.propertyTitle.ifBlank { "Property details" }
                                else -> "New message"
                            }

                            showIncomingMessageFloatingUI(
                                context = context,
                                chatId = chatId,
                                senderId = otherUserId,
                                senderName = senderName,
                                messageText = text
                            )
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        chatsRef.addValueEventListener(listener)
        isListeningToMessages = true
    }
}

