package com.example.gharbato.repository

import com.example.gharbato.model.UserModel
import com.google.firebase.auth.FirebaseUser

interface UserRepo {

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    )
    fun signUp(
        email: String, password: String,
        callback: (Boolean, String, String) -> Unit
    )

    fun addUserToDatabase(
        userId: String,
        model: UserModel, callback: (Boolean, String) -> Unit
    )

    fun forgotPassword(email: String, callback: (Boolean, String) -> Unit)


}