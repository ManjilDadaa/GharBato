package com.example.gharbato.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.model.NotificationModel
import com.example.gharbato.model.UserModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UserRepoImpl : UserRepo {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ref: DatabaseReference = database.getReference("Users")

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )

    // Store listeners so we can remove them later
    private var notificationsListener: ValueEventListener? = null
    private var unreadCountListener: ValueEventListener? = null
    private var notificationsRef: DatabaseReference? = null
    private var unreadCountRef: DatabaseReference? = null

    override fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Login Successful" else it.exception?.message ?: "Failed")
        }
    }

    override fun logout(callback: (Boolean, String) -> Unit) {
        try {
            auth.signOut()
            callback(true, "Logged out successfully")
        } catch (e: Exception) {
            callback(false, e.message ?: "Logout failed")
        }
    }

    override fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNo: String,
        selectedCountry: String,
        callback: (Boolean, String, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            callback(
                it.isSuccessful,
                if (it.isSuccessful) "Success" else it.exception?.message ?: "Failed",
                auth.currentUser?.uid ?: ""
            )
        }
    }

    override fun addUserToDatabase(userId: String, model: UserModel, callback: (Boolean, String) -> Unit) {
        ref.child(userId).setValue(model).addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Success" else "Failed")
        }
    }

    override fun forgotPassword(email: String, callback: (Boolean, String) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Email sent" else "Failed")
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getUser(userId: String, callback: (UserModel?) -> Unit) {
        ref.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) = callback(s.getValue(UserModel::class.java))
            override fun onCancelled(e: DatabaseError) = callback(null)
        })
    }

    override fun updateUserName(userId: String, newName: String, callback: (Boolean, String) -> Unit) {
        ref.child(userId).child("fullName").setValue(newName).addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Name updated" else "Failed")
        }
    }

    override fun updateUserProfile(
        userId: String,
        newName: String,
        profileImageUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>("fullName" to newName)
        if (profileImageUrl.isNotEmpty()) updates["profileImageUrl"] = profileImageUrl
        ref.child(userId).updateChildren(updates).addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Profile updated" else "Failed")
        }
    }

    override fun uploadProfileImage(context: Context, imageUri: Uri, callback: (String?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val response = cloudinary.uploader().upload(
                    context.contentResolver.openInputStream(imageUri),
                    ObjectUtils.asMap("resource_type", "image")
                )
                Handler(Looper.getMainLooper()).post {
                    callback((response["url"] as String?)?.replace("http://", "https://"))
                }
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
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(c: PhoneAuthCredential) =
                        callback(true, "Verification completed", null)

                    override fun onVerificationFailed(e: FirebaseException) =
                        callback(false, e.message ?: "Failed", null)

                    override fun onCodeSent(id: String, t: PhoneAuthProvider.ForceResendingToken) =
                        callback(true, "OTP sent", id)
                }).build()
        )
    }

    override fun verifyOtp(verificationId: String, otpCode: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithCredential(PhoneAuthProvider.getCredential(verificationId, otpCode))
            .addOnCompleteListener {
                callback(it.isSuccessful, if (it.isSuccessful) "Verified" else "Failed")
            }
    }

    override fun sendEmailVerification(callback: (Boolean, String) -> Unit) {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
            callback(it.isSuccessful, if (it.isSuccessful) "Verification email sent" else "Failed")
        }
    }

    override fun checkEmailVerified(callback: (Boolean) -> Unit) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            callback(auth.currentUser?.isEmailVerified == true)
        }
    }

    override fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                callback(
                    true,
                    s.children.mapNotNull {
                        it.getValue(UserModel::class.java)?.copy(userId = it.key ?: "")
                    },
                    "Success"
                )
            }

            override fun onCancelled(e: DatabaseError) = callback(false, null, e.message)
        })
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        getAllUsers { success, users, _ ->
            if (success && users != null) {
                callback(
                    true,
                    users.filter {
                        it.fullName.contains(query, true) ||
                                it.email.contains(query, true) ||
                                it.phoneNo.contains(query, true)
                    },
                    "Success"
                )
            } else {
                callback(false, null, "Failed")
            }
        }
    }

    // ==================== REAL-TIME NOTIFICATION OBSERVERS ====================

    override fun observeNotifications(userId: String, callback: (List<NotificationModel>) -> Unit) {
        removeNotificationObservers()

        notificationsRef = database.getReference("Notifications").child(userId)

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("NotificationRepo", "📱 Notifications changed! Total: ${snapshot.childrenCount}")

                val list = snapshot.children.mapNotNull {
                    it.getValue(NotificationModel::class.java)?.copy(notificationId = it.key ?: "")
                }.sortedByDescending { it.timestamp }

                val unreadCount = list.count { !it.isRead }
                Log.d("NotificationRepo", "✅ Loaded ${list.size} notifications, $unreadCount unread")

                // Log each notification status
                list.forEach {
                    Log.d("NotificationRepo", "  - ${it.title}: isRead=${it.isRead}")
                }

                callback(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationRepo", "❌ Error observing notifications: ${error.message}")
                callback(emptyList())
            }
        }

        notificationsRef?.addValueEventListener(notificationsListener!!)
        Log.d("NotificationRepo", "🎧 Started listening to notifications for user: $userId")
    }

    override fun observeUnreadCount(userId: String, callback: (Int) -> Unit) {
        unreadCountRef = database.getReference("Notifications").child(userId)

        unreadCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count {
                    it.getValue(NotificationModel::class.java)?.isRead == false
                }
                Log.d("NotificationRepo", "🔔 Unread count: $count")
                callback(count)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationRepo", "❌ Error observing unread count: ${error.message}")
                callback(0)
            }
        }

        unreadCountRef?.addValueEventListener(unreadCountListener!!)
        Log.d("NotificationRepo", "🎧 Started listening to unread count for user: $userId")
    }

    override fun removeNotificationObservers() {
        notificationsListener?.let { listener ->
            notificationsRef?.removeEventListener(listener)
            Log.d("NotificationRepo", "🛑 Removed notifications listener")
        }
        notificationsListener = null
        notificationsRef = null

        unreadCountListener?.let { listener ->
            unreadCountRef?.removeEventListener(listener)
            Log.d("NotificationRepo", "🛑 Removed unread count listener")
        }
        unreadCountListener = null
        unreadCountRef = null
    }

    // ==================== NOTIFICATION ACTIONS ====================

    override fun getUserNotifications(
        userId: String,
        callback: (Boolean, List<NotificationModel>?, String) -> Unit
    ) {
        database.getReference("Notifications").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull {
                        it.getValue(NotificationModel::class.java)?.copy(notificationId = it.key ?: "")
                    }.sortedByDescending { it.timestamp }
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
                    val count = snapshot.children.count {
                        it.getValue(NotificationModel::class.java)?.isRead == false
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
        Log.d("NotificationRepo", "📝 Marking notification as read: $notificationId")

        database.getReference("Notifications")
            .child(userId)
            .child(notificationId)
            .child("isRead")
            .setValue(true)
            .addOnSuccessListener {
                Log.d("NotificationRepo", "✅ Notification marked as read successfully")
                callback(true, "Marked as read")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationRepo", "❌ Failed to mark as read: ${e.message}")
                callback(false, "Failed")
            }
    }

    override fun markAllNotificationsAsRead(userId: String, callback: (Boolean, String) -> Unit) {
        Log.d("NotificationRepo", "📝 Starting Mark ALL notifications as read for user: $userId")

        val notifRef = database.getReference("Notifications").child(userId)

        notifRef.get().addOnSuccessListener { snapshot ->
            Log.d("NotificationRepo", "📊 Total notifications in Firebase: ${snapshot.childrenCount}")

            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                Log.d("NotificationRepo", "⚠️ No notifications found")
                callback(true, "No notifications to mark")
                return@addOnSuccessListener
            }

            val unreadNotifications = snapshot.children.filter { child ->
                val isRead = child.child("isRead").getValue(Boolean::class.java) ?: true
                !isRead
            }

            Log.d("NotificationRepo", "📋 Found ${unreadNotifications.size} unread notifications")

            if (unreadNotifications.isEmpty()) {
                Log.d("NotificationRepo", "✅ All notifications already read")
                callback(true, "All notifications already read")
                return@addOnSuccessListener
            }

            // Update each notification individually to guarantee Firebase triggers listeners
            var completed = 0
            var failed = 0
            val total = unreadNotifications.size

            unreadNotifications.forEach { child ->
                Log.d("NotificationRepo", "  📝 Marking ${child.key} as read...")

                child.ref.child("isRead").setValue(true)
                    .addOnSuccessListener {
                        completed++
                        Log.d("NotificationRepo", "  ✅ Marked ${child.key} as read ($completed/$total)")

                        if (completed + failed == total) {
                            Log.d("NotificationRepo", "🎉 All done! Success: $completed, Failed: $failed")
                            callback(true, "Marked $completed as read")
                        }
                    }
                    .addOnFailureListener { e ->
                        failed++
                        Log.e("NotificationRepo", "  ❌ Failed ${child.key}: ${e.message}")

                        if (completed + failed == total) {
                            callback(completed > 0, "Marked $completed, failed $failed")
                        }
                    }
            }

        }.addOnFailureListener { e ->
            Log.e("NotificationRepo", "❌ Failed to fetch notifications: ${e.message}")
            callback(false, "Failed to fetch notifications")
        }
    }

    override fun deleteNotification(
        userId: String,
        notificationId: String,
        callback: (Boolean, String) -> Unit
    ) {
        Log.d("NotificationRepo", "🗑️ Deleting notification: $notificationId")

        database.getReference("Notifications")
            .child(userId)
            .child(notificationId)
            .removeValue()
            .addOnSuccessListener {
                Log.d("NotificationRepo", "✅ Notification deleted successfully")
                callback(true, "Notification deleted")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationRepo", "❌ Failed to delete: ${e.message}")
                callback(false, "Failed")
            }
    }

    fun deleteAllNotifications(userId: String, callback: (Boolean, String) -> Unit) {
        Log.d("NotificationRepo", "🗑️ Deleting ALL notifications for user: $userId")

        val notifRef = database.getReference("Notifications").child(userId)

        notifRef.removeValue()
            .addOnSuccessListener {
                Log.d("NotificationRepo", "✅ All notifications deleted successfully")
                callback(true, "All notifications cleared")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationRepo", "❌ Failed to clear notifications: ${e.message}")
                callback(false, "Failed to clear")
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
        Log.d("NotificationRepo", "📤 Creating notification for user: $userId")

        database.getReference("Notifications").child(userId).push().setValue(
            hashMapOf<String, Any>(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "imageUrl" to imageUrl,
                "actionData" to actionData
            )
        ).addOnSuccessListener {
            Log.d("NotificationRepo", "✅ Notification created successfully")
            callback(true, "Notification created")
        }.addOnFailureListener { e ->
            Log.e("NotificationRepo", "❌ Failed to create notification: ${e.message}")
            callback(false, "Failed")
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
        ref.get().addOnSuccessListener { snapshot ->
            val userIds = snapshot.children.mapNotNull { it.key }
            if (userIds.isEmpty()) {
                callback(false, "No users found")
                return@addOnSuccessListener
            }

            var completed = 0
            var failed = 0

            val notificationData = hashMapOf<String, Any>(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "imageUrl" to imageUrl,
                "actionData" to actionData
            )

            userIds.forEach { uid ->
                database.getReference("Notifications").child(uid).push().setValue(notificationData)
                    .addOnSuccessListener { completed++ }
                    .addOnFailureListener { failed++ }
                    .addOnCompleteListener {
                        if (completed + failed == userIds.size) {
                            callback(
                                true,
                                "Notified $completed/${userIds.size} users" +
                                        if (failed > 0) " ($failed failed)" else ""
                            )
                        }
                    }
            }
        }.addOnFailureListener {
            callback(false, "Failed to fetch users")
        }
    }
}