package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import com.example.gharbato.model.MessageUser
import com.example.gharbato.model.CallRequest
import com.example.gharbato.model.UserModel
import com.example.gharbato.view.ZegoCallActivity
import com.example.gharbato.view.MessageDetailsActivity
import com.google.firebase.auth.FirebaseAuth

interface MessageRepository {
    fun getOrCreateLocalUserId(context: Context): String
    fun getCurrentUser(): MessageUser?
    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit)
    fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean)
    fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String)
}

class MessageRepositoryImpl : MessageRepository {
    
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
        // Delegate to existing UserRepoImpl
        val userRepo = com.example.gharbato.repository.UserRepoImpl()
        userRepo.getAllUsers(callback)
    }
    
    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        // Delegate to existing UserRepoImpl
        val userRepo = com.example.gharbato.repository.UserRepoImpl()
        userRepo.searchUsers(query, callback)
    }
    
    override fun initiateCall(activity: Activity, targetUserId: String, targetUserName: String, isVideoCall: Boolean) {
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
            targetUserId = "", // Not needed for direct ZegoCloud
            isIncomingCall = false
        )
        activity.startActivity(intent)
    }
    
    override fun navigateToChat(activity: Activity, targetUserId: String, targetUserName: String) {
        val intent = MessageDetailsActivity.newIntent(
            activity = activity,
            otherUserId = targetUserId,
            otherUserName = targetUserName,
        )
        activity.startActivity(intent)
    }
}
