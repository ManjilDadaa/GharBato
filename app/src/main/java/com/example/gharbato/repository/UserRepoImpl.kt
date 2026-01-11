package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.model.UserModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UserRepoImpl : UserRepo{

    val auth : FirebaseAuth = FirebaseAuth.getInstance()
    val database : FirebaseDatabase = FirebaseDatabase.getInstance()
    val ref : DatabaseReference = database.getReference("Users")

    // Cloudinary configuration
    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    override fun login(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        ref.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val isSuspended = snapshot.child("isSuspended").getValue(Boolean::class.java) ?: false
                                val suspendedUntil = snapshot.child("suspendedUntil").getValue(Long::class.java) ?: 0L
                                val suspensionReason = snapshot.child("suspensionReason").getValue(String::class.java) ?: ""
                                
                                android.util.Log.d("LoginCheck", "User: $userId, isSuspended: $isSuspended, until: $suspendedUntil")

                                if (isSuspended) {
                                    val currentTime = System.currentTimeMillis()
                                    if (suspendedUntil > currentTime) {
                                        // User is suspended
                                        auth.signOut()
                                        val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                            .format(java.util.Date(suspendedUntil))
                                        callback(false, "Account suspended until $date. Reason: $suspensionReason")
                                    } else {
                                        // Suspension expired, lift it
                                        ref.child(userId).child("isSuspended").setValue(false)
                                        ref.child(userId).child("suspendedUntil").setValue(0)
                                        ref.child(userId).child("suspensionReason").setValue("")
                                        callback(true, "Login Successful")
                                    }
                                } else {
                                    callback(true, "Login Successful")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                android.util.Log.e("LoginCheck", "Database error: ${error.message}")
                                callback(true, "Login Successful") // Proceed if check fails, or handle error
                            }
                        })
                    } else {
                        callback(true, "Login Successful")
                    }
                }
                else{
                    val exception = task.exception
                    when(exception){
                        is FirebaseAuthInvalidUserException -> {
                            callback(false,task.exception?.message.toString())
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            callback(false, "Invalid email or password")
                        }
                        else -> callback(false, "${task.exception?.message}")
                    }
                }
            }
    }

    override fun signUp(
        email: String,
        password: String,
        fullName : String,
        phoneNo: String,
        country : String,
        callback: (Boolean, String, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    callback(true,"Registration Successful", "${auth.currentUser?.uid}")
                }
                else{
                    callback(false, "${it.exception?.message}","" )
                }
            }
    }

    override fun addUserToDatabase(
        userId: String,
        model: UserModel,
        callback: (Boolean, String) -> Unit
    ) {
        ref.child(userId).setValue(model).addOnCompleteListener {
            if (it.isSuccessful){
                callback(true,"User registered successfully")
            }
            else{
                callback(false,"${it.exception?.message}")
            }
        }
    }

    override fun forgotPassword(
        email: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, "Reset Email sent successfully")
                } else {
                    callback(false, "${it.exception?.message}")
                }
            }
    }

    // ---------- PROFILE ----------

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun getUser(
        userId: String,
        callback: (UserModel?) -> Unit
    ) {
        ref.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    callback(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    override fun updateUserName(
        userId: String,
        fullName: String,
        callback: (Boolean, String) -> Unit
    ) {
        val updates = mapOf<String, Any>(
            "fullName" to fullName
        )

        ref.child(userId)
            .updateChildren(updates)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, "Profile updated successfully")
                } else {
                    callback(false, it.exception?.message ?: "Update failed")
                }
            }
    }

    // ---------- NEW: PROFILE WITH IMAGE ----------

    override fun updateUserProfile(
        userId: String,
        fullName: String,
        profileImageUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName
        )

        if (profileImageUrl.isNotEmpty()) {
            updates["profileImageUrl"] = profileImageUrl
        }

        ref.child(userId)
            .updateChildren(updates)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, "Profile updated successfully")
                } else {
                    callback(false, it.exception?.message ?: "Update failed")
                }
            }
    }

    override fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        callback: (String?) -> Unit
    ) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val fileName = "profile_${auth.currentUser?.uid}_${System.currentTimeMillis()}"

                val response = cloudinary.uploader().upload(
                    inputStream, ObjectUtils.asMap(
                        "public_id", fileName,
                        "resource_type", "image",
                        "folder", "profile_images"
                    )
                )

                var imageUrl = response["url"] as String?
                imageUrl = imageUrl?.replace("http://", "https://")

                Handler(Looper.getMainLooper()).post {
                    callback(imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback(null)
                }
            }
        }
    }

    // ---------- OTP & VERIFICATION ----------

    override fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    callback(true, "Verification completed automatically", null)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    callback(
                        false,
                        exception.message ?: "Verification failed",
                        null
                    )
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    callback(true, "OTP sent successfully", verificationId)
                }

                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    super.onCodeAutoRetrievalTimeOut(verificationId)
                    callback(true, "Enter OTP manually", verificationId)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun verifyOtp(
        verificationId: String,
        otpCode: String,
        callback: (Boolean, String) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Phone Number Verified Successfully")
                } else {
                    when (val exception = task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            callback(false, "Invalid OTP code")
                        }
                        else -> {
                            callback(false, exception?.message ?: "Verification failed")
                        }
                    }
                }
            }
    }

    override fun sendEmailVerification(callback: (Boolean, String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(true, "Verification email sent to ${user.email}")
                    } else {
                        callback(false, task.exception?.message ?: "Failed to send verification email")
                    }
                }
        } else {
            callback(false, "No user logged in")
        }
    }

    override fun checkEmailVerified(callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(user.isEmailVerified)
                } else {
                    callback(false)
                }
            }
        } else {
            callback(false)
        }
    }

    override fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        android.util.Log.d("UserRepoImpl", "Fetching all registered users from Firebase Database")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserRepoImpl", "Database snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                val userList = mutableListOf<UserModel>()

                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    android.util.Log.d("UserRepoImpl", "User found: ${userSnapshot.key} = ${userModel}")

                    if (userModel != null) {
                        val userWithId = userModel.copy(userId = userSnapshot.key ?: "")
                        userList.add(userWithId)
                        android.util.Log.d("UserRepoImpl", "Added user: ${userWithId.fullName} (${userWithId.email})")
                    }
                }

                android.util.Log.d("UserRepoImpl", "Returning ${userList.size} registered users")
                callback(true, userList, "Registered users fetched successfully")
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("UserRepoImpl", "Database error: ${error.message}")
                callback(false, null, "Database error: ${error.message}")
            }
        })
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        android.util.Log.d("UserRepoImpl", "Searching users with query: '$query' from Firebase Database")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserRepoImpl", "Search - Database snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                val userList = mutableListOf<UserModel>()

                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    android.util.Log.d("UserRepoImpl", "Search - User found: ${userSnapshot.key} = ${userModel}")

                    if (userModel != null) {
                        val userWithId = userModel.copy(userId = userSnapshot.key ?: "")

                        if (query.isBlank() ||
                            userWithId.fullName.contains(query, ignoreCase = true) ||
                            userWithId.email.contains(query, ignoreCase = true) ||
                            userWithId.phoneNo.contains(query, ignoreCase = true)) {
                            userList.add(userWithId)
                            android.util.Log.d("UserRepoImpl", "Search adding user: ${userWithId.fullName}")
                        }
                    }
                }

                android.util.Log.d("UserRepoImpl", "Search returning ${userList.size} users")
                callback(true, userList, "Search completed successfully")
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("UserRepoImpl", "Search database error: ${error.message}")
                callback(false, null, "Database error: ${error.message}")
            }
        })
    }

    // ---------- NOTIFICATIONS ----------

    override fun getUserNotifications(
        userId: String,
        callback: (Boolean, List<com.example.gharbato.model.NotificationModel>?, String) -> Unit
    ) {
        val notificationsRef = database.getReference("Notifications").child(userId)

        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<com.example.gharbato.model.NotificationModel>()

                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(com.example.gharbato.model.NotificationModel::class.java)
                    if (notification != null) {
                        notifications.add(notification.copy(notificationId = notificationSnapshot.key ?: ""))
                    }
                }

                // Sort by timestamp (newest first)
                notifications.sortByDescending { it.timestamp }
                callback(true, notifications, "Notifications fetched successfully")
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, null, "Error: ${error.message}")
            }
        })
    }

    override fun markNotificationAsRead(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val notificationRef = database.getReference("Notifications")
            .child(userId)
            .child(notificationId)

        notificationRef.updateChildren(mapOf("isRead" to true))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Notification marked as read")
                } else {
                    callback(false, "Failed to update notification")
                }
            }
    }

    override fun markAllNotificationsAsRead(
        userId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val notificationsRef = database.getReference("Notifications").child(userId)

        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = mutableMapOf<String, Any>()

                for (notificationSnapshot in snapshot.children) {
                    updates["${notificationSnapshot.key}/isRead"] = true
                }

                if (updates.isNotEmpty()) {
                    notificationsRef.updateChildren(updates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                callback(true, "All notifications marked as read")
                            } else {
                                callback(false, "Failed to update notifications")
                            }
                        }
                } else {
                    callback(true, "No notifications to update")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, "Error: ${error.message}")
            }
        })
    }

    override fun deleteNotification(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val notificationRef = database.getReference("Notifications")
            .child(userId)
            .child(notificationId)

        notificationRef.removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Notification deleted")
                } else {
                    callback(false, "Failed to delete notification")
                }
            }
    }

    override fun getUnreadNotificationCount(
        userId: String,
        callback: (Int) -> Unit
    ) {
        val notificationsRef = database.getReference("Notifications").child(userId)

        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0

                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(com.example.gharbato.model.NotificationModel::class.java)
                    if (notification != null && !notification.isRead) {
                        unreadCount++
                    }
                }

                callback(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(0)
            }
        })
    }

    // ---------- NOTIFICATION CREATION ----------

    override fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String,
        actionData: String,
        callback: (Boolean, String) -> Unit
    ) {
        val notificationRef = database.getReference("Notifications").child(userId)
        val newNotificationRef = notificationRef.push()

        val notification = mapOf(
            "title" to title,
            "message" to message,
            "type" to type,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "imageUrl" to imageUrl,
            "actionData" to actionData
        )

        newNotificationRef.setValue(notification)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Notification created")
                } else {
                    callback(false, "Failed to create notification")
                }
            }
    }

    override fun notifyAllUsers(
        title: String,
        message: String,
        type: String,
        imageUrl: String,
        actionData: String,
        callback: (Boolean, String) -> Unit
    ) {
        // Get all user IDs
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var successCount = 0
                var totalUsers = 0

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    totalUsers++

                    // Create notification for each user
                    createNotification(
                        userId = userId,
                        title = title,
                        message = message,
                        type = type,
                        imageUrl = imageUrl,
                        actionData = actionData
                    ) { success, _ ->
                        if (success) successCount++

                        // Check if all notifications sent
                        if (successCount + (totalUsers - successCount) == totalUsers) {
                            callback(true, "Notified $successCount users")
                        }
                    }
                }

                if (totalUsers == 0) {
                    callback(false, "No users to notify")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, "Error: ${error.message}")
            }
        })
    }
}