package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.CallRequest
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

    companion object {
        private const val TAG = "MessageRepositoryImpl"
    }

    private val auth = FirebaseAuth.getInstance()

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
        val userRepo = com.example.gharbato.repository.UserRepoImpl()
        userRepo.getAllUsers(callback)
    }

    override fun getChatPartners(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return callback(false, null, "No current user found")

        Log.d(TAG, "Getting chat partners for user: $currentUserId")

        val database = FirebaseDatabase.getInstance()
        val chatsRef = database.getReference("chats")

        chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatPartnerIds = mutableSetOf<String>()

                Log.d(TAG, "Total chat rooms found: ${snapshot.childrenCount}")

                // Find all chat rooms that involve current user
                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach

                    Log.d(TAG, "Checking chat room: $chatId")

                    // Check if current user is part of this chat
                    if (chatId.contains(currentUserId)) {
                        // Extract the other user's ID from chatId
                        // chatId format: "userId1_userId2" (sorted)
                        val userIds = chatId.split("_")
                        for (userId in userIds) {
                            if (userId != currentUserId && userId.isNotBlank()) {
                                chatPartnerIds.add(userId)
                                Log.d(TAG, "Found chat partner: $userId")
                            }
                        }
                    }
                }

                Log.d(TAG, "Total unique chat partners: ${chatPartnerIds.size}")

                if (chatPartnerIds.isEmpty()) {
                    callback(true, emptyList(), "")
                    return
                }

                // Get user details for each chat partner
                val userRepo = com.example.gharbato.repository.UserRepoImpl()
                val allUsers = mutableListOf<UserModel>()
                var completedQueries = 0

                chatPartnerIds.forEach { partnerId ->
                    userRepo.getUserById(partnerId) { success, user, _ ->
                        completedQueries++
                        if (success && user != null) {
                            allUsers.add(user)
                            Log.d(TAG, "Loaded user details: ${user.fullName} (${user.userId})")
                        }

                        // When all user queries are complete, return the result
                        if (completedQueries == chatPartnerIds.size) {
                            Log.d(TAG, "Returning ${allUsers.size} chat partners")
                            callback(true, allUsers, "")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load chat partners: ${error.message}")
                callback(false, null, "Failed to load chat partners: ${error.message}")
            }
        })
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        val userRepo = com.example.gharbato.repository.UserRepoImpl()
        userRepo.searchUsers(query, callback)
    }

    override fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
        val currentUserName = auth.currentUser?.email ?: "Me"

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

    override fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String, targetUserImage: String) {
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
}