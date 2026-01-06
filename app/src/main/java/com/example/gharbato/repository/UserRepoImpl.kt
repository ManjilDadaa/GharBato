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
            .addOnCompleteListener {
                if (it.isSuccessful){
                    callback(true,"Login Successful")
                }
                else{
                    val exception = it.exception
                    when(exception){
                        is FirebaseAuthInvalidUserException -> {
                            callback(false,it.exception?.message.toString())
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            callback(false, "Invalid email or password")
                        }
                        else -> callback(false, "${it.exception?.message}")
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

    override fun getUserById(
        userId: String,
        callback: (Boolean, UserModel?, String) -> Unit
    ) {
        ref.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        callback(true, user, "")
                    } else {
                        callback(false, null, "User not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, null, "Database error: ${error.message}")
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
}