package com.example.gharbato.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.NotificationModel
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepo
import com.example.gharbato.utils.NotificationHelper

class UserViewModel(val repo: UserRepo) : ViewModel() {

    private val _userData = MutableLiveData<UserModel?>()
    val userData: LiveData<UserModel?> get() = _userData

    private val _profileUpdateStatus = MutableLiveData<Pair<Boolean, String>>()
    val profileUpdateStatus: LiveData<Pair<Boolean, String>> get() = _profileUpdateStatus

    private val _imageUploadStatus = MutableLiveData<String?>()
    val imageUploadStatus: LiveData<String?> get() = _imageUploadStatus

    private val _notifications = MutableLiveData<List<NotificationModel>>()
    val notifications: LiveData<List<NotificationModel>> get() = _notifications

    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> get() = _unreadCount

    // Store previous notification IDs to detect new ones
    private val previousNotificationIds = mutableSetOf<String>()

    // Store context for showing notifications
    private var appContext: Context? = null

    // ==================== AUTHENTICATION ====================

    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        repo.login(email, password, callback)
    }

    fun loginWithGoogle(idToken: String, callback: (Boolean, String, Boolean) -> Unit) {
        repo.loginWithGoogle(idToken, callback)
    }

    fun logout(callback: (Boolean, String) -> Unit) {
        repo.logout(callback)
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNo: String,
        selectedCountry: String,
        callback: (Boolean, String, String) -> Unit
    ) {
        repo.signUp(email, password, fullName, phoneNo, selectedCountry, callback)
    }

    fun addUserToDatabase(
        userId: String,
        model: UserModel,
        callback: (Boolean, String) -> Unit
    ) {
        repo.addUserToDatabase(userId, model, callback)
    }

    fun forgotPassword(email: String, callback: (Boolean, String) -> Unit) {
        repo.forgotPassword(email, callback)
    }

    fun getCurrentUserId(): String? {
        return repo.getCurrentUserId()
    }

    // ==================== USER PROFILE ====================

    fun loadUserProfile() {
        val userId = repo.getCurrentUserId() ?: return
        repo.getUser(userId) { user ->
            _userData.postValue(user)
        }
    }

    fun updateUserName(newName: String) {
        val userId = repo.getCurrentUserId() ?: return
        repo.updateUserName(userId, newName) { success, message ->
            _profileUpdateStatus.postValue(Pair(success, message))
            if (success) loadUserProfile()
        }
    }

    fun updateUserProfile(newName: String, profileImageUrl: String) {
        val userId = repo.getCurrentUserId() ?: return
        repo.updateUserProfile(userId, newName, profileImageUrl) { success, message ->
            _profileUpdateStatus.postValue(Pair(success, message))
            if (success) loadUserProfile()
        }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri) {
        repo.uploadProfileImage(context, imageUri) { imageUrl ->
            _imageUploadStatus.postValue(imageUrl)
        }
    }

    // ==================== PHONE & EMAIL VERIFICATION ====================

    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callback: (Boolean, String, String?) -> Unit
    ) {
        repo.sendOtp(phoneNumber, activity, callback)
    }

    fun verifyOtp(
        verificationId: String,
        otpCode: String,
        callback: (Boolean, String) -> Unit
    ) {
        repo.verifyOtp(verificationId, otpCode, callback)
    }

    fun sendEmailVerification(callback: (Boolean, String) -> Unit) {
        repo.sendEmailVerification(callback)
    }

    fun checkEmailVerified(callback: (Boolean) -> Unit) {
        repo.checkEmailVerified(callback)
    }

    fun checkIsSuspended(callback: (Boolean, String?, Long?) -> Unit) {
        val userId = repo.getCurrentUserId() ?: run {
            callback(false, null, null)
            return
        }
        repo.getUser(userId) { user ->
            if (user?.isSuspended == true) {
                callback(true, user.suspensionReason, user.suspendedUntil)
            } else {
                callback(false, null, null)
            }
        }
    }

    // ==================== USER SEARCH ====================

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        repo.getAllUsers(callback)
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        repo.searchUsers(query, callback)
    }

    // ==================== REAL-TIME NOTIFICATION OBSERVERS ====================

    /**
     * Initialize context for showing device notifications
     */
    fun initializeNotificationContext(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Start observing notifications with device notification support
     */
    fun startObservingNotifications() {
        val userId = repo.getCurrentUserId() ?: return

        repo.observeNotifications(userId) { notificationList ->
            // Detect new notifications
            val newNotifications = notificationList.filter { notification ->
                !previousNotificationIds.contains(notification.notificationId)
            }

            // Show device notifications for new unread notifications
            appContext?.let { context ->
                newNotifications.forEach { notification ->
                    if (!notification.isRead) {
                        NotificationHelper.showNotification(context, notification)
                    }
                }
            }

            // Update the previous IDs set
            previousNotificationIds.clear()
            previousNotificationIds.addAll(notificationList.map { it.notificationId })

            // Update LiveData
            _notifications.postValue(notificationList)
        }

        repo.observeUnreadCount(userId) { count ->
            _unreadCount.postValue(count)
        }
    }

    fun stopObservingNotifications() {
        repo.removeNotificationObservers()
        previousNotificationIds.clear()
    }

    // ==================== NOTIFICATION ACTIONS ====================

    fun loadNotifications() {
        val userId = repo.getCurrentUserId() ?: return
        repo.getUserNotifications(userId) { success, notificationList, _ ->
            if (success && notificationList != null) {
                // Don't trigger device notifications on manual load
                previousNotificationIds.clear()
                previousNotificationIds.addAll(notificationList.map { it.notificationId })
                _notifications.postValue(notificationList)
            }
        }
    }

    fun loadUnreadCount() {
        val userId = repo.getCurrentUserId() ?: return
        repo.getUnreadNotificationCount(userId) { count ->
            _unreadCount.postValue(count)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val userId = repo.getCurrentUserId() ?: return
        repo.markNotificationAsRead(userId, notificationId) { _, _ -> }
    }

    fun markAllAsRead() {
        val userId = repo.getCurrentUserId() ?: return
        repo.markAllNotificationsAsRead(userId) { _, _ -> }
    }

    fun deleteNotification(notificationId: String) {
        val userId = repo.getCurrentUserId() ?: return
        repo.deleteNotification(userId, notificationId) { _, _ -> }
    }

    // ==================== CREATE NOTIFICATIONS ====================

    /**
     * Create notification for current user WITH callback
     */
    fun createNotification(
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = "",
        callback: (Boolean, String) -> Unit
    ) {
        val userId = repo.getCurrentUserId() ?: run {
            callback(false, "User not logged in")
            return
        }
        repo.createNotification(userId, title, message, type, imageUrl, actionData, callback)
    }

    /**
     * Create notification for specific user WITH callback
     */
    fun createNotificationForUser(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = "",
        callback: (Boolean, String) -> Unit
    ) {
        repo.createNotification(userId, title, message, type, imageUrl, actionData, callback)
    }

    /**
     * Create notification WITHOUT callback (fire and forget)
     */
    fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = ""
    ) {
        repo.createNotification(userId, title, message, type, imageUrl, actionData) { _, _ -> }
    }

    /**
     * Notify all users
     */
    fun notifyAllUsers(
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = "",
        callback: (Boolean, String) -> Unit
    ) {
        repo.notifyAllUsers(title, message, type, imageUrl, actionData, callback)
    }

    // ==================== LIFECYCLE ====================

    override fun onCleared() {
        super.onCleared()
        stopObservingNotifications()
        appContext = null
    }
}