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
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Create user model and save to database
                        val userModel = UserModel(
                            userId = userId,
                            email = email,
                            fullName = fullName,
                            phoneNo = phoneNo,
                            selectedCountry = country
                        )
                        
                        addUserToDatabase(userId, userModel) { success, message ->
                            if (success) {
                                android.util.Log.d("UserRepoImpl", "User saved to database: $fullName")
                                callback(true, "Registration Successful", userId)
                            } else {
                                android.util.Log.e("UserRepoImpl", "Failed to save user to database: $message")
                                callback(false, "Registration successful but failed to save to database: $message", "")
                            }
                        }
                    } else {
                        callback(false, "User ID is null after registration", "")
                    }
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
        android.util.Log.d("UserRepoImpl", "Fetching all registered users from Firebase Database")
        
        // Fetch registered users from Firebase Database
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserRepoImpl", "Database snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                val userList = mutableListOf<UserModel>()
                
                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    android.util.Log.d("UserRepoImpl", "User found: ${userSnapshot.key} = ${userModel}")
                    
                    if (userModel != null) {
                        // Add the userId from the snapshot key
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
        
        // Fetch registered users from Firebase Database
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserRepoImpl", "Search - Database snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                val userList = mutableListOf<UserModel>()
                
                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    android.util.Log.d("UserRepoImpl", "Search - User found: ${userSnapshot.key} = ${userModel}")
                    
                    if (userModel != null) {
                        val userWithId = userModel.copy(userId = userSnapshot.key ?: "")
                        
                        // Filter based on search query
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