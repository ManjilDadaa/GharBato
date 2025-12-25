package com.example.gharbato.viewmodel

import android.app.Activity
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
//        email: String, password: String,fullName : String, phoneNo: String, selectedCountry : String,
        email: String, password: String,fullName : String,
        callback: (Boolean, String, String) -> Unit
    ){
//        repo.signUp(email,password,fullName,phoneNo, selectedCountry, callback)
        repo.signUp(email,password,fullName, callback)
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
    fun sendOtp(phoneNumber: String,
                activity: Activity, callback: (Boolean, String, String?) -> Unit
    ){
        repo.sendOtp(phoneNumber, activity){
            success, message, verificationId ->
            callback(success, message, verificationId)
        }
    }

    fun verifyOtp(verificationId: String,
                  otpCode: String,
                  callback: (Boolean, String) -> Unit
    ){
        repo.verifyOtp(verificationId, otpCode){
            success, message ->
            callback(success, message)
        }
    }
    fun sendEmailVerification(callback: (Boolean, String) -> Unit){
        repo.sendEmailVerification(callback)
    }
    fun checkEmailVerified(callback: (Boolean) -> Unit){
        repo.checkEmailVerified(callback)
    }




    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.getAllUsers(callback)
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.searchUsers(query, callback)
    }

}