package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.UserModel
import com.example.gharbato.view.ZegoCallActivity
import com.example.gharbato.view.MessageDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

interface MessageRepository {
    fun getOrCreateLocalUserId(context: Context): String
    fun getCurrentUser(): MessageUser?
    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean)
    fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String, targetUserImage: String = "")
}

class MessageRepositoryImpl : MessageRepository {
    
    private val auth = FirebaseAuth.getInstance()
    
    private fun sanitizeZegoId(value: String): String {
        if (value.isBlank()) return "user"
        return value.replace(Regex("[^A-Za-z0-9_]"), "_")
    }
    
    private fun buildChatId(userA: String, userB: String): String {
        val a = sanitizeZegoId(userA)
        val b = sanitizeZegoId(userB)
        return if (a <= b) "${a}_$b" else "${b}_$a"
    }
    
    override fun getOrCreateLocalUserId(context: Context): String {
        val prefs = context.getSharedPreferences("gharbato_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("local_user_id", null)
        if (!existing.isNullOrBlank()) return existing

        val newId = "guest_${System.currentTimeMillis()}"
        prefs.edit().putString("local_user_id", newId).apply()
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
        // Delegate to existing UserRepoImpl
        val userRepo = UserRepoImpl()
        userRepo.getAllUsers(callback)
    }
    
    override fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return callback(false, null, "No current user found")
        
        val database = FirebaseDatabase.getInstance()
        val chatsRef = database.getReference("chats")
        
        chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatPartnerIds = mutableSetOf<String>()
                
                // Find all chat rooms that involve current user
                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach
                    
                    // Check if current user is part of this chat
                    if (chatId.contains(currentUserId)) {
                        // Extract the other user's ID from chatId
                        val userIds = chatId.split("_")
                        for (userId in userIds) {
                            if (userId != currentUserId && userId.isNotBlank()) {
                                chatPartnerIds.add(userId)
                            }
                        }
                    }
                }
                
                if (chatPartnerIds.isEmpty()) {
                    callback(true, emptyList(), "")
                    return
                }
                
                // Get user details for each chat partner
                val userRepo = UserRepoImpl()
                val allUsers = mutableListOf<UserModel>()
                var completedQueries = 0
                
                if (chatPartnerIds.isEmpty()) {
                    callback(true, emptyList(), "")
                    return
                }
                
                chatPartnerIds.forEach { partnerId ->
                    userRepo.getUser(partnerId) { user ->
                        completedQueries++
                        if (user != null) {
                            allUsers.add(user.copy(userId = partnerId))
                        }
                        
                        // When all user queries are complete, return the result
                        if (completedQueries == chatPartnerIds.size) {
                            callback(true, allUsers, "")
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, null, "Failed to load chat partners: ${error.message}")
            }
        })
    }
    
    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        // Delegate to existing UserRepoImpl
        val userRepo = UserRepoImpl()
        userRepo.searchUsers(query, callback)
    }
    
    override fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
        val currentUserName = auth.currentUser?.email ?: "Me"
        
        val callId = buildChatId(currentUserId, targetUserId)
        
        val intent = ZegoCallActivity.newIntent(
            activity = activity,
            callId = callId,
            userId = currentUserId,
            userName = currentUserName,
            isVideoCall = isVideoCall,
            targetUserId = targetUserId,
            isIncomingCall = false
        )
        activity.startActivity(intent)
    }
    
    override fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String, targetUserImage: String) {
        val intent = MessageDetailsActivity.newIntent(
            activity = activity,
            otherUserId = targetUserId,
            otherUserName = targetUserName,
            otherUserImage = targetUserImage
        )
        activity.startActivity(intent)
    }
}
