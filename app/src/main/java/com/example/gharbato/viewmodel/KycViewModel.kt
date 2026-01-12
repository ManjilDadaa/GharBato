package com.example.gharbato.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.KycModel
import com.example.gharbato.repository.KycRepo

class KycViewModel(private val repo: KycRepo) : ViewModel() {
    
    private val _kycSubmissions = MutableLiveData<List<KycModel>>()
    val kycSubmissions: LiveData<List<KycModel>> get() = _kycSubmissions
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading
    
    fun submitKyc(
        userId: String,
        userEmail: String,
        userName: String,
        documentType: String,
        frontImageUri: Uri,
        backImageUri: Uri,
        context: Context,
        callback: (Boolean, String) -> Unit
    ) {
        _loading.value = true
        repo.submitKyc(userId, userEmail, userName, documentType, frontImageUri, backImageUri, context) { success, message ->
            _loading.value = false
            callback(success, message)
        }
    }
    
    fun loadAllKycSubmissions() {
        _loading.value = true
        repo.getAllKycSubmissions { success, kycList, _ ->
            _loading.value = false
            if (success && kycList != null) {
                _kycSubmissions.value = kycList
            }
        }
    }
    
    fun updateKycStatus(
        kycId: String,
        status: String,
        reviewedBy: String,
        rejectionReason: String = "",
        callback: (Boolean, String) -> Unit
    ) {
        repo.updateKycStatus(kycId, status, reviewedBy, rejectionReason) { success, message ->
            callback(success, message)
            if (success) {
                loadAllKycSubmissions() // Refresh the list
            }
        }
    }
    
    fun getUserKycStatus(userId: String, callback: (KycModel?) -> Unit) {
        repo.getUserKycStatus(userId, callback)
    }
}