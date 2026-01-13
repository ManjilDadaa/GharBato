package com.example.gharbato.repository

import android.content.Context
import android.net.Uri
import com.example.gharbato.model.KycModel

interface KycRepo {
    fun submitKyc(
        userId: String,
        userEmail: String,
        userName: String,
        documentType: String,
        frontImageUri: Uri,
        backImageUri: Uri,
        context: Context,
        callback: (Boolean, String) -> Unit
    )
    
    fun getAllKycSubmissions(callback: (Boolean, List<KycModel>?, String) -> Unit)
    
    fun updateKycStatus(
        kycId: String,
        status: String,
        reviewedBy: String,
        rejectionReason: String = "",
        callback: (Boolean, String) -> Unit
    )
    
    fun getUserKycStatus(userId: String, callback: (KycModel?) -> Unit)
}