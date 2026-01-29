package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.gharbato.model.NotificationModel
import com.example.gharbato.model.UserModel

interface UserRepo {
    fun login(email: String, password: String, callback: (Boolean, String) -> Unit)

    fun logout(callback: (Boolean, String) -> Unit)

    // Google Sign-In: callback returns (success, message, isNewUser)
    fun loginWithGoogle(
        idToken: String,
        callback: (Boolean, String, Boolean) -> Unit
    )

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNo: String,
        selectedCountry: String,
        callback: (Boolean, String, String) -> Unit
    )

    fun addUserToDatabase(
        userId: String,
        model: UserModel,
        callback: (Boolean, String) -> Unit
    )

    fun forgotPassword(email: String, callback: (Boolean, String) -> Unit)

    fun getCurrentUserId(): String?

    fun getUser(userId: String, callback: (UserModel?) -> Unit)

    fun updateUserName(userId: String, newName: String, callback: (Boolean, String) -> Unit)

    fun updateUserProfile(
        userId: String,
        newName: String,
        profileImageUrl: String,
        callback: (Boolean, String) -> Unit
    )

    fun uploadProfileImage(context: Context, imageUri: Uri, callback: (String?) -> Unit)

    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callback: (Boolean, String, String?) -> Unit
    )

    fun verifyOtp(
        verificationId: String,
        otpCode: String,
        callback: (Boolean, String) -> Unit
    )

    fun sendEmailVerification(callback: (Boolean, String) -> Unit)

    fun checkEmailVerified(callback: (Boolean) -> Unit)

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit)

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit)

    // Real-time notification observers
    fun observeNotifications(userId: String, callback: (List<NotificationModel>) -> Unit)

    fun observeUnreadCount(userId: String, callback: (Int) -> Unit)

    fun removeNotificationObservers()

    // Notification actions
    fun getUserNotifications(
        userId: String,
        callback: (Boolean, List<NotificationModel>?, String) -> Unit
    )

    fun getUnreadNotificationCount(userId: String, callback: (Int) -> Unit)

    fun markNotificationAsRead(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    )

    fun markAllNotificationsAsRead(userId: String, callback: (Boolean, String) -> Unit)

    fun deleteNotification(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    )

    fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String,
        actionData: String,
        callback: (Boolean, String) -> Unit
    )

    fun notifyAllUsers(
        title: String,
        message: String,
        type: String,
        imageUrl: String,
        actionData: String,
        callback: (Boolean, String) -> Unit
    )
}