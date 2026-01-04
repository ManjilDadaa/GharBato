package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.gharbato.model.UserModel

interface UserRepo {

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    )
    fun signUp(
        email: String, password: String, fullName : String, phoneNo : String, country : String,
        callback: (Boolean, String, String) -> Unit
    )

    fun addUserToDatabase(
        userId: String,
        model: UserModel, callback: (Boolean, String) -> Unit
    )

    fun forgotPassword(email: String,
                       callback: (Boolean, String) -> Unit)

    fun getCurrentUserId(): String?

    fun getUser(
        userId: String,
        callback: (UserModel?) -> Unit
    )

    fun updateUserName(
        userId: String,
        fullName: String,
        callback: (Boolean, String) -> Unit
    )


    fun updateUserProfile(
        userId: String,
        fullName: String,
        profileImageUrl: String,
        callback: (Boolean, String) -> Unit
    )


    fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    )

    fun sendOtp(phoneNumber: String,
                activity: Activity, callback: (Boolean, String, String?) -> Unit
    )

    fun verifyOtp(verificationId: String,
                  otpCode: String,
                  callback: (Boolean, String) -> Unit
    )

    fun sendEmailVerification(callback: (Boolean, String) -> Unit)

    fun checkEmailVerified(callback: (Boolean) -> Unit)

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit)

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit)

}