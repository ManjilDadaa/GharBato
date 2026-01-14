package com.example.gharbato.model

data class KycModel(
    val kycId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val documentType: String = "",
    val frontImageUrl: String = "",
    val backImageUrl: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val submittedAt: Long = 0L,
    val reviewedAt: Long = 0L,
    val reviewedBy: String = "",
    val rejectionReason: String = ""
)