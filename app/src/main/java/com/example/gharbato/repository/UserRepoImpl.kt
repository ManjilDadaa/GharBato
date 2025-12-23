package com.example.gharbato.repository

import com.example.gharbato.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserRepoImpl : UserRepo{

    val auth : FirebaseAuth = FirebaseAuth.getInstance()
    val database : FirebaseDatabase = FirebaseDatabase.getInstance()
    val ref : DatabaseReference = database.getReference("Users")
    // Try alternative paths if Users doesn't work
    val altRef1 : DatabaseReference = database.getReference("users")
    val altRef2 : DatabaseReference = database.reference

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

    override fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit) {
        android.util.Log.d("UserRepoImpl", "Fetching all users from Firebase path: ${ref.path}")
        
        // First, try to debug the database structure
        altRef2.get().addOnSuccessListener { rootSnapshot ->
            android.util.Log.d("UserRepoImpl", "Root database children: ${rootSnapshot.children.map { it.key }}")
            
            // Try the main Users path
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    android.util.Log.d("UserRepoImpl", "Firebase snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                    val userList = mutableListOf<UserModel>()
                    for (userSnapshot in snapshot.children) {
                        val userModel = userSnapshot.getValue(UserModel::class.java)
                        android.util.Log.d("UserRepoImpl", "User snapshot key: ${userSnapshot.key}, value: ${userSnapshot.value}")
                        if (userModel != null) {
                            userList.add(userModel.copy(userId = userSnapshot.key ?: ""))
                        }
                    }
                    
                    // If no users found in "Users", try "users"
                    if (userList.isEmpty()) {
                        android.util.Log.d("UserRepoImpl", "No users found in 'Users', trying 'users' path")
                        altRef1.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(altSnapshot: DataSnapshot) {
                                android.util.Log.d("UserRepoImpl", "Alt path 'users' exists: ${altSnapshot.exists()}, children count: ${altSnapshot.childrenCount}")
                                val altUserList = mutableListOf<UserModel>()
                                for (userSnapshot in altSnapshot.children) {
                                    val userModel = userSnapshot.getValue(UserModel::class.java)
                                    android.util.Log.d("UserRepoImpl", "Alt User snapshot key: ${userSnapshot.key}, value: ${userSnapshot.value}")
                                    if (userModel != null) {
                                        altUserList.add(userModel.copy(userId = userSnapshot.key ?: ""))
                                    }
                                }
                                android.util.Log.d("UserRepoImpl", "Returning ${altUserList.size} users from alt path")
                                callback(true, altUserList, "Users fetched successfully from alt path")
                            }
                            
                            override fun onCancelled(error: DatabaseError) {
                                android.util.Log.e("UserRepoImpl", "Alt path database error: ${error.message}")
                                callback(true, userList, "Users fetched successfully from main path (empty)")
                            }
                        })
                    } else {
                        android.util.Log.d("UserRepoImpl", "Returning ${userList.size} users")
                        callback(true, userList, "Users fetched successfully")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("UserRepoImpl", "Database error: ${error.message}")
                    callback(false, null, "Database error: ${error.message}")
                }
            })
        }.addOnFailureListener { error ->
            android.util.Log.e("UserRepoImpl", "Failed to get root snapshot: ${error.message}")
            callback(false, null, "Failed to access database: ${error.message}")
        }
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        android.util.Log.d("UserRepoImpl", "Searching users with query: '$query' from Firebase path: ${ref.path}")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserRepoImpl", "Search - Firebase snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                val userList = mutableListOf<UserModel>()
                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    android.util.Log.d("UserRepoImpl", "Search - User snapshot key: ${userSnapshot.key}, value: ${userSnapshot.value}")
                    if (userModel != null) {
                        val userWithId = userModel.copy(userId = userSnapshot.key ?: "")
                        if (query.isBlank() || 
                            userWithId.fullName.contains(query, ignoreCase = true) ||
                            userWithId.email.contains(query, ignoreCase = true) ||
                            userWithId.phoneNo.contains(query, ignoreCase = true)) {
                            android.util.Log.d("UserRepoImpl", "Adding user to results: ${userWithId.fullName}")
                            userList.add(userWithId)
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