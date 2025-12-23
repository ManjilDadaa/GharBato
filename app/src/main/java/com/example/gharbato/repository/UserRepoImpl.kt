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
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<UserModel>()
                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    if (userModel != null) {
                        userList.add(userModel.copy(userId = userSnapshot.key ?: ""))
                    }
                }
                callback(true, userList, "Users fetched successfully")
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, null, "Database error: ${error.message}")
            }
        })
    }

    override fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<UserModel>()
                for (userSnapshot in snapshot.children) {
                    val userModel = userSnapshot.getValue(UserModel::class.java)
                    if (userModel != null) {
                        val userWithId = userModel.copy(userId = userSnapshot.key ?: "")
                        if (query.isBlank() || 
                            userWithId.fullName.contains(query, ignoreCase = true) ||
                            userWithId.email.contains(query, ignoreCase = true) ||
                            userWithId.phoneNo.contains(query, ignoreCase = true)) {
                            userList.add(userWithId)
                        }
                    }
                }
                callback(true, userList, "Search completed successfully")
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, null, "Database error: ${error.message}")
            }
        })
    }
}