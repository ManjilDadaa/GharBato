package com.example.gharbato.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepo

class UserViewModel (val repo: UserRepo) : ViewModel(){

    private val _userData = MutableLiveData<UserModel?>()
    val userData: LiveData<UserModel?> get() = _userData

    private val _profileUpdateStatus = MutableLiveData<Pair<Boolean, String>>()
    val profileUpdateStatus: LiveData<Pair<Boolean, String>> get() = _profileUpdateStatus

    // NEW: LiveData for image upload status
    private val _imageUploadStatus = MutableLiveData<String?>()
    val imageUploadStatus: LiveData<String?> get() = _imageUploadStatus

    fun login(
        email: String, password: String,
        callback: (Boolean, String) -> Unit
    ){
        repo.login(email, password, callback)
    }

    fun signUp(
        email: String, password: String,
        fullName : String,
        phoneNo: String,
        selectedCountry : String,
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

    fun forgotPassword(email: String,
                       callback: (Boolean, String) -> Unit){
        repo.forgotPassword(email,callback)
    }

    // ------------------ PROFILE ------------------

    fun getCurrentUserId(): String? {
        return repo.getCurrentUserId()
    }

    fun loadUserProfile() {
        val userId = repo.getCurrentUserId() ?: return

        repo.getUser(userId) { user ->
            _userData.postValue(user)
        }
    }

    fun updateUserName(newName: String) {
        val userId = repo.getCurrentUserId() ?: return

        repo.updateUserName(userId, newName) { success, message ->
            _profileUpdateStatus.postValue(Pair(success, message))
            if (success) {
                loadUserProfile()
            }
        }
    }



    fun updateUserProfile(newName: String, profileImageUrl: String) {
        val userId = repo.getCurrentUserId() ?: return

        repo.updateUserProfile(userId, newName, profileImageUrl) { success, message ->
            _profileUpdateStatus.postValue(Pair(success, message))
            if (success) {
                loadUserProfile()
            }
        }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri) {
        repo.uploadProfileImage(context, imageUri) { imageUrl ->
            _imageUploadStatus.postValue(imageUrl)
        }
    }

    // ------------------ OTP & VERIFICATION ------------------

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

    // ------------------ USER MANAGEMENT ------------------

    fun getAllUsers(callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.getAllUsers(callback)
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserModel>?, String) -> Unit){
        repo.searchUsers(query, callback)
    }
}