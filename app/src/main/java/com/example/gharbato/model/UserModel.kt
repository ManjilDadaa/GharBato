package com.example.gharbato.model

import com.google.firebase.database.PropertyName

data class UserModel(
    val userId : String = "",
    val email : String = "",
    val fullName : String = "",
    val phoneNo : String = "",
    val selectedCountry : String = "",
    val profileImageUrl : String ="",
//    val dob : String = "",
    @get:PropertyName("isSuspended")
    val isSuspended: Boolean = false,
    val suspendedUntil: Long = 0L,
    val suspensionReason: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val isOnline: Boolean = false,
    val lastActive: Long = 0L
)

