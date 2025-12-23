package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepo

class UserViewModel (val repo: UserRepo) : ViewModel(){

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    ){
        repo.login(email, password, callback)
    }
    fun signUp(
        email: String, password: String,fullName : String, phoneNo: String, selectedCountry : String,
        callback: (Boolean, String, String) -> Unit
    ){
        repo.signUp(email,password,fullName,phoneNo, selectedCountry, callback)
    }

    fun addUserToDatabase(
        userId: String,
        model: UserModel, callback: (Boolean, String) -> Unit
    ){
        repo.addUserToDatabase(userId, model, callback)
    }

    fun forgotPassword(email: String, callback: (Boolean, String) -> Unit){
        repo.forgotPassword(email,callback)
    }

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.getAllUsers(callback)
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.searchUsers(query, callback)
    }

}