package com.example.gharbato.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepo

class UserViewModel (val repo: UserRepo) : ViewModel(){

    private val _userData = MutableLiveData<UserModel?>()
    val userData: LiveData<UserModel?> get() = _userData

    private val _profileUpdateStatus = MutableLiveData<Pair<Boolean, String>>()
    val profileUpdateStatus: LiveData<Pair<Boolean, String>> get() = _profileUpdateStatus

    // NEW: LiveData for image upload status
    private val _imageUploadStatus = MutableLiveData<String?>()
    val imageUploadStatus: LiveData<String?> get() = _imageUploadStatus

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    ){
        repo.login(email, password, callback)
    }

    fun signUp(
        email: String, password: String,
        fullName : String,
        phoneNo: String,
        selectedCountry : String,
        callback: (Boolean, String, String) -> Unit
    ){
        repo.signUp(email,password,fullName,phoneNo, selectedCountry, callback)
    }

    fun addUserToDatabase(
        userId: String,
        model: UserModel, callback: (Boolean, String) -> Unit
    ){
        repo.addUserToDatabase(userId, model, callback)
    }

    fun forgotPassword(email: String,
                       callback: (Boolean, String) -> Unit){
        repo.forgotPassword(email,callback)
    }

    // ------------------ PROFILE ------------------

    fun getCurrentUserId(): String? {
        return repo.getCurrentUserId()
    }

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
            if (success) {
                loadUserProfile()
            }
        }
    }

    // ---------- NEW: PROFILE WITH IMAGE ----------

    fun updateUserProfile(newName: String, profileImageUrl: String) {
        val userId = repo.getCurrentUserId() ?: return

        repo.updateUserProfile(userId, newName, profileImageUrl) { success, message ->
            _profileUpdateStatus.postValue(Pair(success, message))
            if (success) {
                loadUserProfile()
            }
        }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri) {
        repo.uploadProfileImage(context, imageUri) { imageUrl ->
            _imageUploadStatus.postValue(imageUrl)
        }
    }

    // ------------------ OTP & VERIFICATION ------------------

    fun sendOtp(phoneNumber: String,
                activity: Activity, callback: (Boolean, String, String?) -> Unit
    ){
        repo.sendOtp(phoneNumber, activity){
                success, message, verificationId ->
            callback(success, message, verificationId)
        }
    }

    fun verifyOtp(verificationId: String,
                  otpCode: String,
                  callback: (Boolean, String) -> Unit
    ){
        repo.verifyOtp(verificationId, otpCode){
                success, message ->
            callback(success, message)
        }
    }

    fun sendEmailVerification(callback: (Boolean, String) -> Unit){
        repo.sendEmailVerification(callback)
    }

    fun checkEmailVerified(callback: (Boolean) -> Unit){
        repo.checkEmailVerified(callback)
    }

    // ------------------ USER MANAGEMENT ------------------

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.getAllUsers(callback)
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.searchUsers(query, callback)
    }

    // ------------------ NOTIFICATIONS ------------------

    private val _notifications = MutableLiveData<List<com.example.gharbato.model.NotificationModel>>()
    val notifications: LiveData<List<com.example.gharbato.model.NotificationModel>> get() = _notifications

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> get() = _unreadCount

    fun loadNotifications() {
        val userId = repo.getCurrentUserId() ?: return

        repo.getUserNotifications(userId) { success, notificationList, message ->
            if (success && notificationList != null) {
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

        repo.markNotificationAsRead(userId, notificationId) { _, _ ->
            loadNotifications()
            loadUnreadCount()
        }
    }

    fun markAllAsRead() {
        val userId = repo.getCurrentUserId() ?: return

        repo.markAllNotificationsAsRead(userId) { _, _ ->
            loadNotifications()
            loadUnreadCount()
        }
    }

    fun deleteNotification(notificationId: String) {
        val userId = repo.getCurrentUserId() ?: return

        repo.deleteNotification(userId, notificationId) { _, _ ->
            loadNotifications()
            loadUnreadCount()
        }
    }

    // ---------- NOTIFICATION CREATION ----------

    fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = ""
    ) {
        repo.createNotification(userId, title, message, type, imageUrl, actionData) { _, _ ->
            // Notification created
        }
    }

    fun notifyAllUsers(
        title: String,
        message: String,
        type: String,
        imageUrl: String = "",
        actionData: String = ""
    ) {
        repo.notifyAllUsers(title, message, type, imageUrl, actionData) { _, _ ->
            // All users notified
        }
    }
}