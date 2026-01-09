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
import com.google.firebase.auth.*
import com.google.firebase.database.*
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UserRepoImpl : UserRepo {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val ref: DatabaseReference = database.getReference("Users")

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    override fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) callback(true, "Login Successful")
                else callback(false, it.exception?.message ?: "Login failed")
            }
    }

    override fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNo: String,
        country: String,
        callback: (Boolean, String, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    callback(true, "Registration Successful", auth.currentUser?.uid ?: "")
                else callback(false, it.exception?.message ?: "Signup failed", "")
            }
    }

    override fun addUserToDatabase(
        userId: String,
        model: UserModel,
        callback: (Boolean, String) -> Unit
    ) {
        ref.child(userId).setValue(model).addOnCompleteListener {
            if (it.isSuccessful) callback(true, "User registered successfully")
            else callback(false, it.exception?.message ?: "Failed")
        }
    }

    override fun forgotPassword(email: String, callback: (Boolean, String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener {
                if (it.isSuccessful) callback(true, "Reset Email sent")
                else callback(false, it.exception?.message ?: "Failed")
            }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getUser(userId: String, callback: (UserModel?) -> Unit) {
        ref.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.getValue(UserModel::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    override fun updateUserName(userId: String, fullName: String, callback: (Boolean, String) -> Unit) {
        ref.child(userId).child("fullName").setValue(fullName)
            .addOnCompleteListener {
                if (it.isSuccessful) callback(true, "Updated")
                else callback(false, it.exception?.message ?: "Failed")
            }
    }

    override fun updateUserProfile(
        userId: String,
        fullName: String,
        profileImageUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>("fullName" to fullName)
        if (profileImageUrl.isNotEmpty()) updates["profileImageUrl"] = profileImageUrl

        ref.child(userId).updateChildren(updates).addOnCompleteListener {
            if (it.isSuccessful) callback(true, "Updated")
            else callback(false, it.exception?.message ?: "Failed")
        }
    }

    override fun uploadProfileImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val response = cloudinary.uploader().upload(
                    inputStream,
                    ObjectUtils.asMap("resource_type", "image")
                )
                val imageUrl = (response["url"] as String?)?.replace("http://", "https://")
                Handler(Looper.getMainLooper()).post { callback(imageUrl) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback(null) }
            }
        }
    }

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
                    callback(true, "Verified", null)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    callback(false, e.message ?: "Failed", null)
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    callback(true, "OTP Sent", id)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun verifyOtp(
        verificationId: String,
        otpCode: String,
        callback: (Boolean, String) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) callback(true, "Verified")
            else callback(false, it.exception?.message ?: "Failed")
        }
    }

    override fun sendEmailVerification(callback: (Boolean, String) -> Unit) {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
            if (it.isSuccessful) callback(true, "Email sent")
            else callback(false, it.exception?.message ?: "Failed")
        }
    }

    override fun checkEmailVerified(callback: (Boolean) -> Unit) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            callback(auth.currentUser?.isEmailVerified == true)
        }
    }

    override fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<UserModel>()
                for (child in snapshot.children) {
                    child.getValue(UserModel::class.java)?.let {
                        users.add(it.copy(userId = child.key ?: ""))
                    }
                }
                callback(true, users, "Success")
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, null, error.message)
            }
        })
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        getAllUsers { success, users, _ ->
            if (!success || users == null) {
                callback(false, null, "Failed")
                return@getAllUsers
            }

            val result = users.filter {
                it.fullName.contains(query, true) ||
                        it.email.contains(query, true) ||
                        it.phoneNo.contains(query, true)
            }

            callback(true, result, "Success")
        }
    }

    override fun getUserNotifications(
        userId: String,
        callback: (Boolean, List<com.example.gharbato.model.NotificationModel>?, String) -> Unit
    ) {
        database.getReference("Notifications").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<com.example.gharbato.model.NotificationModel>()
                    for (child in snapshot.children) {
                        val n =
                            child.getValue(com.example.gharbato.model.NotificationModel::class.java)
                        if (n != null) list.add(n.copy(notificationId = child.key ?: ""))
                    }
                    list.sortByDescending { it.timestamp }
                    callback(true, list, "Success")
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, null, error.message)
                }
            })
    }

    override fun getUnreadNotificationCount(userId: String, callback: (Int) -> Unit) {
        database.getReference("Notifications").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (child in snapshot.children) {
                        val n =
                            child.getValue(com.example.gharbato.model.NotificationModel::class.java)
                        if (n != null && !n.isRead) count++
                    }
                    callback(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(0)
                }
            })
    }

    override fun markNotificationAsRead(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    ) {
        database.getReference("Notifications")
            .child(userId)
            .child(notificationId)
            .child("isRead")
            .setValue(true)
            .addOnCompleteListener {
                callback(it.isSuccessful, "Updated")
            }
    }

    override fun markAllNotificationsAsRead(
        userId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val ref = database.getReference("Notifications").child(userId)
        ref.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>()
            for (child in snapshot.children) {
                updates["${child.key}/isRead"] = true
            }
            if (updates.isEmpty()) callback(true, "Done")
            else ref.updateChildren(updates).addOnCompleteListener {
                callback(it.isSuccessful, "Done")
            }
        }
    }

    override fun deleteNotification(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    ) {
        database.getReference("Notifications")
            .child(userId)
            .child(notificationId)
            .removeValue()
            .addOnCompleteListener {
                callback(it.isSuccessful, "Deleted")
            }
    }

    override fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        imageUrl: String,
        actionData: String,
        callback: (Boolean, String) -> Unit
    ) {
        val ref = database.getReference("Notifications").child(userId).push()
        ref.setValue(
            mapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "imageUrl" to imageUrl,
                "actionData" to actionData
            )
        ).addOnCompleteListener {
            callback(it.isSuccessful, "Created")
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
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (user in snapshot.children) {
                    createNotification(
                        user.key ?: return,
                        title,
                        message,
                        type,
                        imageUrl,
                        actionData
                    ) { _, _ -> }
                }
                callback(true, "Sent")
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, error.message)
            }
        })
    }
}
