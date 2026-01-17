package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.gharbato.model.ChatMessage
import com.example.gharbato.model.ChatSession
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.UserModel
import com.example.gharbato.view.ZegoCallActivity
import com.example.gharbato.view.MessageDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.model.PropertyModel
import java.io.InputStream
import java.util.concurrent.Executors

interface MessageRepository {
    fun getOrCreateLocalUserId(context: Context): String
    fun getCurrentUser(): MessageUser?
    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean)
    fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String, targetUserImage: String = "")

    // Chat Session Management
    fun createChatSession(context: Context, otherUserId: String): ChatSession
    fun listenToBlockStatus(myUserId: String, otherUserId: String, callback: (Boolean, Boolean) -> Unit): () -> Unit
    fun listenToChatMessages(chatId: String, onMessages: (List<ChatMessage>) -> Unit): () -> Unit
    fun sendTextMessage(chatId: String, senderId: String, senderName: String, text: String)
    fun sendImageMessage(context: Context, chatId: String, senderId: String, senderName: String, imageUri: Uri)
    fun blockUser(myUserId: String, otherUserId: String)
    fun unblockUser(myUserId: String, otherUserId: String)
    fun deleteChat(chatId: String)

    fun navigateToChatWithMessage(
        activity: Activity,
        targetUserId: String,
        targetUserName: String,
        targetUserImage: String = "",
        initialMessage: String = ""
    )


    fun listenToTotalUnreadCount(userId: String, onCountChange: (Int) -> Unit): () -> Unit
    fun markMessagesAsRead(chatId: String, currentUserId: String)
    fun sendQuickMessage(
        context: Context,
        otherUserId: String,
        message: String,
        onComplete: () -> Unit
    )
    fun sendQuickMessageWithProperty(
        context: Context,
        otherUserId: String,
        message: String,
        property: PropertyModel,
        onComplete: () -> Unit
    )

    fun sendQuickMessageWithPropertyAndNavigate(
        context: Context,
        activity: Activity,
        otherUserId: String,
        otherUserName: String,
        otherUserImage: String,
        message: String,
        property: PropertyModel
    )


}

class MessageRepositoryImpl : MessageRepository {

    companion object {
        private const val TAG = "MessageRepositoryImpl"
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    // Cloudinary configuration
    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    override fun getOrCreateLocalUserId(context: Context): String {
        val prefs = context.getSharedPreferences("gharbato_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("local_user_id", null)
        if (!existing.isNullOrBlank()) return existing

        val newId = "guest_${System.currentTimeMillis()}"
        prefs.edit().putString("local_user_id", newId).apply()
        Log.d(TAG, "Created new local user ID: $newId")
        return newId
    }

    override fun getCurrentUser(): MessageUser? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            MessageUser(
                userId = currentUser.uid,
                userName = currentUser.email ?: "",
                displayName = currentUser.email ?: "Me"
            )
        } else {
            null
        }
    }

    override fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        Log.d(TAG, "Getting all users")
        val userRepo = UserRepoImpl()
        userRepo.getAllUsers(callback)
    }

    override fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Log.e(TAG, "No current user found when getting chat partners")
            callback(false, null, "No current user found")
            return
        }

        Log.d(TAG, "Getting chat partners for user: $currentUserId")

        val chatsRef = database.getReference("chats")

        chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatPartnerIds = mutableSetOf<String>()

                Log.d(TAG, "Total chat rooms found: ${snapshot.childrenCount}")

                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue

                    if (chatId.contains(currentUserId)) {
                        val userIds = chatId.split("_")
                        for (userId in userIds) {
                            if (userId != currentUserId && userId.isNotBlank()) {
                                chatPartnerIds.add(userId)
                            }
                        }
                    }
                }

                Log.d(TAG, "Total unique chat partners: ${chatPartnerIds.size}")

                if (chatPartnerIds.isEmpty()) {
                    callback(true, emptyList(), "")
                    return
                }

                fetchUsersByIds(chatPartnerIds.toList()) { users ->
                    if (users.isEmpty()) {
                        callback(true, emptyList(), "")
                        return@fetchUsersByIds
                    }

                    val usersWithLastMessage = mutableListOf<UserModel>()
                    var remaining = users.size

                    for (user in users) {
                        val sortedIds = listOf(currentUserId, user.userId).sorted()
                        val chatId = "${sortedIds[0]}_${sortedIds[1]}"
                        val messagesRef = database.getReference("chats")
                            .child(chatId)
                            .child("messages")

                        messagesRef
                            .orderByChild("timestamp")
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(messageSnapshot: DataSnapshot) {
                                    var lastMessageText = ""
                                    var lastTimestamp = 0L

                                    val lastMsg = messageSnapshot.children.firstOrNull()
                                        ?.getValue(ChatMessage::class.java)

                                    if (lastMsg != null) {
                                        lastMessageText = when {
                                            lastMsg.text.isNotBlank() -> lastMsg.text
                                            lastMsg.imageUrl.isNotBlank() -> "Photo"
                                            lastMsg.hasPropertyCard -> lastMsg.propertyTitle
                                            else -> ""
                                        }
                                        lastTimestamp = lastMsg.timestamp
                                    }

                                    val updatedUser = user.copy(
                                        lastMessage = lastMessageText,
                                        lastMessageTimestamp = lastTimestamp
                                    )
                                    usersWithLastMessage.add(updatedUser)

                                    remaining--
                                    if (remaining == 0) {
                                        callback(true, usersWithLastMessage, "")
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "Failed to load last message for chat $chatId: ${error.message}")
                                    usersWithLastMessage.add(user)
                                    remaining--
                                    if (remaining == 0) {
                                        callback(true, usersWithLastMessage, "")
                                    }
                                }
                            })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load chat partners: ${error.message}")
                callback(false, null, "Failed to load chat partners: ${error.message}")
            }
        })
    }

    private fun fetchUsersByIds(userIds: List<String>, callback: (List<UserModel>) -> Unit) {
        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }

        Log.d(TAG, "Fetching details for ${userIds.size} users")

        val usersRef = database.getReference("Users")
        val allUsers = mutableListOf<UserModel>()
        var completedQueries = 0

        for (userId in userIds) {
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        // Ensure userId is set from the key if missing in the data
                        val userWithId = if (user.userId.isBlank()) {
                            user.copy(userId = userId)
                        } else {
                            user
                        }
                        allUsers.add(userWithId)
                        Log.d(TAG, "Loaded user details: ${userWithId.fullName} (${userWithId.userId})")
                    } else {
                        Log.w(TAG, "User not found: $userId")
                    }

                    completedQueries++
                    if (completedQueries == userIds.size) {
                        Log.d(TAG, "Completed loading all users: ${allUsers.size} successful")
                        callback(allUsers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load user $userId: ${error.message}")
                    completedQueries++
                    if (completedQueries == userIds.size) {
                        callback(allUsers)
                    }
                }
            })
        }
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        Log.d(TAG, "Searching users with query: $query")
        val userRepo = UserRepoImpl()
        userRepo.searchUsers(query, callback)
    }

    override fun initiateCall(
        activity: Activity,
        targetUserId: String,
        targetUserName: String,
        isVideoCall: Boolean
    ) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
        val currentUserName = auth.currentUser?.email ?: "Me"

        Log.d(TAG, "=== Initiating Call ===")
        Log.d(TAG, "Current User ID: $currentUserId")
        Log.d(TAG, "Current User Name: $currentUserName")
        Log.d(TAG, "Target User ID: $targetUserId")
        Log.d(TAG, "Target User Name: $targetUserName")
        Log.d(TAG, "Is Video Call: $isVideoCall")

        // Use current user ID as room ID for direct call
        val callId = currentUserId

        val intent = ZegoCallActivity.newIntent(
            activity = activity,
            callId = callId,
            userId = currentUserId,
            userName = currentUserName,
            isVideoCall = isVideoCall,
            targetUserId = "",
            isIncomingCall = false
        )
        activity.startActivity(intent)
    }

    override fun navigateToChat(
        activity: Activity,
        targetUserId: String,
        targetUserName: String,
        targetUserImage: String
    ) {
        Log.d(TAG, "=== Navigating to Chat ===")
        Log.d(TAG, "Target User ID: $targetUserId")
        Log.d(TAG, "Target User Name: $targetUserName")
        Log.d(TAG, "Target User Image: $targetUserImage")

        val intent = MessageDetailsActivity.newIntent(
            activity = activity,
            otherUserId = targetUserId,
            otherUserName = targetUserName,
            otherUserImage = targetUserImage
        )
        activity.startActivity(intent)
    }

    // Chat Session Management Implementation

    override fun createChatSession(context: Context, otherUserId: String): ChatSession {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
        val currentUserName = auth.currentUser?.email ?: "Me"
        
        val sortedIds = listOf(currentUserId, otherUserId).sorted()
        val chatId = "${sortedIds[0]}_${sortedIds[1]}"
        
        return ChatSession(
            chatId = chatId,
            myUserId = currentUserId,
            myUserName = currentUserName,
            otherUserId = otherUserId
        )
    }

    override fun listenToBlockStatus(myUserId: String, otherUserId: String, callback: (Boolean, Boolean) -> Unit): () -> Unit {
        val blockedRef = database.getReference("blocks")
        
        // Listener for "Am I blocked by them?"
        val blockedByOtherListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isBlocked = snapshot.child(otherUserId).child(myUserId).exists()
                // We need to fetch the other status too, or manage state. 
                // For simplicity, let's just trigger a check
                checkBothBlockStatuses(myUserId, otherUserId, callback)
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        // Listener for "Did I block them?"
        val blockedByMeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                checkBothBlockStatuses(myUserId, otherUserId, callback)
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        blockedRef.child(otherUserId).child(myUserId).addValueEventListener(blockedByOtherListener)
        blockedRef.child(myUserId).child(otherUserId).addValueEventListener(blockedByMeListener)

        return {
            blockedRef.child(otherUserId).child(myUserId).removeEventListener(blockedByOtherListener)
            blockedRef.child(myUserId).child(otherUserId).removeEventListener(blockedByMeListener)
        }
    }

    private fun checkBothBlockStatuses(myUserId: String, otherUserId: String, callback: (Boolean, Boolean) -> Unit) {
        val blockedRef = database.getReference("blocks")
        
        blockedRef.child(otherUserId).child(myUserId).get().addOnSuccessListener { snapshot1 ->
            val isBlockedByOther = snapshot1.exists()
            
            blockedRef.child(myUserId).child(otherUserId).get().addOnSuccessListener { snapshot2 ->
                val isBlockedByMe = snapshot2.exists()
                callback(isBlockedByMe, isBlockedByOther)
            }
        }
    }

    override fun listenToChatMessages(chatId: String, onMessages: (List<ChatMessage>) -> Unit): () -> Unit {
        val messagesRef = database.getReference("chats").child(chatId).child("messages")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<ChatMessage>()
                snapshot.children.forEach { msgSnapshot ->
                    try {
                        val message = msgSnapshot.getValue(ChatMessage::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }
                onMessages(messageList.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load messages: ${error.message}")
            }
        }
        
        messagesRef.addValueEventListener(listener)
        
        return {
            messagesRef.removeEventListener(listener)
        }
    }

    override fun sendTextMessage(chatId: String, senderId: String, senderName: String, text: String) {
        val messagesRef = database.getReference("chats").child(chatId).child("messages")
        val messageId = messagesRef.push().key ?: return
        
        val message = ChatMessage(
            id = messageId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        
        messagesRef.child(messageId).setValue(message)
    }

    override fun sendImageMessage(context: Context, chatId: String, senderId: String, senderName: String, imageUri: Uri) {
        Log.d(TAG, "Starting image upload for chat $chatId")
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open input stream for $imageUri")
                    return@execute
                }

                val fileName = "chat_${chatId}_${System.currentTimeMillis()}"

                val response = cloudinary.uploader().upload(
                    inputStream, ObjectUtils.asMap(
                        "public_id", fileName,
                        "folder", "chat_images"
                    )
                )
                // Use secure_url to ensure HTTPS, which is required by Android by default
                val imageUrl = response["secure_url"] as String? ?: response["url"] as String?

                if (imageUrl != null) {
                    Log.d(TAG, "Image uploaded successfully: $imageUrl")
                    val messagesRef = database.getReference("chats").child(chatId).child("messages")
                    val messageId = messagesRef.push().key ?: return@execute
                    
                    val message = ChatMessage(
                        id = messageId,
                        senderId = senderId,
                        senderName = senderName,
                        text = "",
                        imageUrl = imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    messagesRef.child(messageId).setValue(message)
                } else {
                    Log.e(TAG, "Image upload failed, url is null")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Error uploading image", e)
            }
        }
    }

    override fun blockUser(myUserId: String, otherUserId: String) {
        val blockedRef = database.getReference("blocks")
        blockedRef.child(myUserId).child(otherUserId).setValue(true)
    }

    override fun unblockUser(myUserId: String, otherUserId: String) {
        val blockedRef = database.getReference("blocks")
        blockedRef.child(myUserId).child(otherUserId).removeValue()
    }

    override fun deleteChat(chatId: String) {
        val chatRef = database.getReference("chats").child(chatId)
        chatRef.removeValue()
    }



    override fun navigateToChatWithMessage(
        activity: Activity,
        targetUserId: String,
        targetUserName: String,
        targetUserImage: String,
        initialMessage: String
    ) {
        Log.d(TAG, "=== Navigating to Chat with Message ===")
        Log.d(TAG, "Target User ID: $targetUserId")
        Log.d(TAG, "Target User Name: $targetUserName")
        Log.d(TAG, "Initial Message: $initialMessage")

        val intent = MessageDetailsActivity.newIntent(
            activity = activity,
            otherUserId = targetUserId,
            otherUserName = targetUserName,
            otherUserImage = targetUserImage
        ).apply {
            putExtra("INITIAL_MESSAGE", initialMessage)
        }
        activity.startActivity(intent)
    }



    override fun listenToTotalUnreadCount(userId: String, onCountChange: (Int) -> Unit): () -> Unit {
        val chatsRef = database.getReference("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalUnread = 0
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    if (chatId.contains(userId)) {
                        val messagesSnapshot = chatSnapshot.child("messages")
                        for (msgSnapshot in messagesSnapshot.children) {
                            try {
                                val msg = msgSnapshot.getValue(ChatMessage::class.java)
                                if (msg != null && msg.senderId != userId && !msg.isRead) {
                                    totalUnread++
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing message for unread count", e)
                            }
                        }
                    }
                }
                onCountChange(totalUnread)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to listen to unread count: ${error.message}")
            }
        }

        chatsRef.addValueEventListener(listener)

        return {
            chatsRef.removeEventListener(listener)
        }
    }

    override fun markMessagesAsRead(chatId: String, currentUserId: String) {
        val messagesRef = database.getReference("chats").child(chatId).child("messages")
        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (msgSnapshot in snapshot.children) {
                    try {
                        val msg = msgSnapshot.getValue(ChatMessage::class.java)
                        if (msg != null && msg.senderId != currentUserId && !msg.isRead) {
                            msgSnapshot.ref.child("isRead").setValue(true)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error marking message as read", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to mark messages as read: ${error.message}")
            }
        })
    }
    override fun sendQuickMessage(
        context: Context,
        otherUserId: String,
        message: String,
        onComplete: () -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
        val currentUserName = auth.currentUser?.email ?: "Me"

        // Create chat session
        val session = createChatSession(context, otherUserId)

        // Send the message immediately
        sendTextMessage(
            chatId = session.chatId,
            senderId = session.myUserId,
            senderName = session.myUserName,
            text = message
        )

        // Callback to notify completion
        onComplete()
    }


    override fun sendQuickMessageWithProperty(
        context: Context,
        otherUserId: String,
        message: String,
        property: PropertyModel,
        onComplete: () -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
        val currentUserName = auth.currentUser?.email ?: "Me"

        // Create chat session
        val session = createChatSession(context, otherUserId)

        // Get the first image from the property
        val propertyImageUrl = property.images.values.flatten().firstOrNull() ?: property.imageUrl

        // Create message with property data
        val messagesRef = database.getReference("chats").child(session.chatId).child("messages")
        val messageId = messagesRef.push().key ?: return

        val chatMessage = hashMapOf(
            "id" to messageId,
            "senderId" to session.myUserId,
            "senderName" to session.myUserName,
            "text" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "imageUrl" to "",
            // Property card data
            "propertyId" to property.id,
            "propertyTitle" to property.developer,
            "propertyPrice" to property.price,
            "propertyImage" to propertyImageUrl,
            "propertyLocation" to property.location,
            "propertyBedrooms" to property.bedrooms,
            "propertyBathrooms" to property.bathrooms
        )

        Log.d(TAG, "Sending message with property card:")
        Log.d(TAG, "Property ID: ${property.id}")
        Log.d(TAG, "Property Title: ${property.developer}")
        Log.d(TAG, "Property Image: $propertyImageUrl")

        messagesRef.child(messageId).setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Property card message sent successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send property card message", e)
            }
    }

    override fun sendQuickMessageWithPropertyAndNavigate(
        context: Context,
        activity: Activity,
        otherUserId: String,
        otherUserName: String,
        otherUserImage: String,
        message: String,
        property: PropertyModel
    ) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
        val currentUserName = auth.currentUser?.email ?: "Me"

        // Create chat session
        val session = createChatSession(context, otherUserId)

        val propertyImageUrl = property.images.values.flatten().firstOrNull() ?: property.imageUrl

        val messagesRef = database.getReference("chats").child(session.chatId).child("messages")
        val messageId = messagesRef.push().key ?: return

        val chatMessage = hashMapOf(
            "id" to messageId,
            "senderId" to session.myUserId,
            "senderName" to session.myUserName,
            "text" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "imageUrl" to "",
            // Property card data
            "propertyId" to property.id,
            "propertyTitle" to property.developer,
            "propertyPrice" to property.price,
            "propertyImage" to propertyImageUrl,
            "propertyLocation" to property.location,
            "propertyBedrooms" to property.bedrooms,
            "propertyBathrooms" to property.bathrooms
        )

        Log.d(TAG, "Sending message with property card and navigating:")
        Log.d(TAG, "Chat ID: ${session.chatId}")
        Log.d(TAG, "Property ID: ${property.id}")
        Log.d(TAG, "Property Title: ${property.developer}")
        Log.d(TAG, "Property Image: $propertyImageUrl")

        // Send message first
        messagesRef.child(messageId).setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Property card message sent successfully, navigating to chat")
                // Small delay to ensure Firebase has processed the write
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val intent = MessageDetailsActivity.newIntent(
                        activity = activity,
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        otherUserImage = otherUserImage
                    )
                    activity.startActivity(intent)
                }, 300)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send property card message", e)
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }



}
