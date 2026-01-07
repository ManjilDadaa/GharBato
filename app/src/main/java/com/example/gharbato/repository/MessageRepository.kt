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
    private val database = FirebaseDatabase.getInstance()

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
        val userRepo = UserRepoImpl()
        userRepo.getAllUsers(callback)
    }

    override fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            callback(false, null, "No current user found")
            return
        }

        val chatsRef = database.getReference("chats")

        chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatPartnerIds = mutableSetOf<String>()

                // Find all chat rooms that involve current user
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue

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
                fetchUsersByIds(chatPartnerIds.toList()) { users ->
                    callback(true, users, "")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, null, "Failed to load chat partners: ${error.message}")
            }
        })
    }

    private fun fetchUsersByIds(userIds: List<String>, callback: (List<UserModel>) -> Unit) {
        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val usersRef = database.getReference("users")
        val allUsers = mutableListOf<UserModel>()
        var completedQueries = 0

        for (userId in userIds) {
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        allUsers.add(user)
                    }

                    completedQueries++
                    if (completedQueries == userIds.size) {
                        callback(allUsers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completedQueries++
                    if (completedQueries == userIds.size) {
                        callback(allUsers)
                    }
                }
            })
        }
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
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
        val intent = MessageDetailsActivity.newIntent(
            activity = activity,
            otherUserId = targetUserId,
            otherUserName = targetUserName,
            otherUserImage = targetUserImage
        )
        activity.startActivity(intent)
    }
}