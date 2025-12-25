package com.example.gharbato.repository

import android.app.Activity
import com.example.gharbato.model.UserModel
import com.google.firebase.auth.FirebaseUser

interface UserRepo {

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    )
    fun signUp(
//        email: String, password: String, fullName : String, phoneNo : String, country : String,
        email: String, password: String, fullName : String,
        callback: (Boolean, String, String) -> Unit
    )

    fun addUserToDatabase(
        userId: String,
        model: UserModel, callback: (Boolean, String) -> Unit
    )

    fun forgotPassword(
        email: String, callback: (Boolean, String) -> Unit
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