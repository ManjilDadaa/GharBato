package com.example.gharbato.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.gharbato.model.KycModel
import com.google.firebase.database.*
import java.util.concurrent.Executors

class KycRepoImpl : KycRepo {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val kycRef: DatabaseReference = database.getReference("KycSubmissions")
    
    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to "dwqybrjf2",
            "api_key" to "929885821451753",
            "api_secret" to "TLkLKEgA67ZkqcfzIyvxPgGpqHE"
        )
    )
    
    override fun submitKyc(
        userId: String,
        userEmail: String,
        userName: String,
        documentType: String,
        frontImageUri: Uri,
        backImageUri: Uri,
        context: Context,
        callback: (Boolean, String) -> Unit
    ) {
        // Upload images first
        uploadImages(context, frontImageUri, backImageUri) { frontUrl, backUrl ->
            if (frontUrl != null && backUrl != null) {
                val kycId = kycRef.push().key ?: return@uploadImages callback(false, "Failed to generate ID")
                
                val kycModel = KycModel(
                    kycId = kycId,
                    userId = userId,
                    userEmail = userEmail,
                    userName = userName,
                    documentType = documentType,
                    frontImageUrl = frontUrl,
                    backImageUrl = backUrl,
                    status = "Pending",
                    submittedAt = System.currentTimeMillis()
                )
                
                kycRef.child(kycId).setValue(kycModel).addOnCompleteListener { task ->
                    callback(task.isSuccessful, if (task.isSuccessful) "KYC submitted successfully" else "Failed to submit")
                }
            } else {
                callback(false, "Failed to upload images")
            }
        }
    }
    
    private fun uploadImages(
        context: Context,
        frontUri: Uri,
        backUri: Uri,
        callback: (String?, String?) -> Unit
    ) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val frontResponse = cloudinary.uploader().upload(
                    context.contentResolver.openInputStream(frontUri),
                    ObjectUtils.asMap("resource_type", "image")
                )
                val backResponse = cloudinary.uploader().upload(
                    context.contentResolver.openInputStream(backUri),
                    ObjectUtils.asMap("resource_type", "image")
                )
                
                Handler(Looper.getMainLooper()).post {
                    callback(
                        (frontResponse["url"] as String?)?.replace("http://", "https://"),
                        (backResponse["url"] as String?)?.replace("http://", "https://")
                    )
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback(null, null) }
            }
        }
    }
    
    override fun getAllKycSubmissions(callback: (Boolean, List<KycModel>?, String) -> Unit) {
        kycRef.orderByChild("submittedAt").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val kycList = snapshot.children.mapNotNull { 
                    it.getValue(KycModel::class.java) 
                }.sortedByDescending { it.submittedAt }
                callback(true, kycList, "Success")
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, null, error.message)
            }
        })
    }
    
    override fun updateKycStatus(
        kycId: String,
        status: String,
        reviewedBy: String,
        rejectionReason: String,
        callback: (Boolean, String) -> Unit
    ) {
        val updates = mapOf(
            "status" to status,
            "reviewedAt" to System.currentTimeMillis(),
            "reviewedBy" to reviewedBy,
            "rejectionReason" to rejectionReason
        )
        
        kycRef.child(kycId).updateChildren(updates).addOnCompleteListener { task ->
            callback(task.isSuccessful, if (task.isSuccessful) "Status updated" else "Failed to update")
        }
    }
    
    override fun getUserKycStatus(userId: String, callback: (KycModel?) -> Unit) {
        kycRef.orderByChild("userId").equalTo(userId).limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val kyc = snapshot.children.firstOrNull()?.getValue(KycModel::class.java)
                    callback(kyc)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }
}