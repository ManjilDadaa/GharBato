package com.example.gharbato.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ReportedProperty(
    val reportId: String = "",
    val propertyId: Int = 0,
    val propertyTitle: String = "",
    val propertyImage: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val reportedByName: String = "",
    val reportedBy = getCurrentUserId(),
    val reportReason: String = "",
    val reportDetails: String = "",
    val reportedAt: Long = System.currentTimeMillis(),
    val status: String = ReportStatus.PENDING
)


object ReportStatus {
    const val PENDING = "PENDING"
    const val REVIEWED = "REVIEWED"
    const val DISMISSED = "DISMISSED"
}

object ReportReasons {
    const val FAKE_LISTING = "Fake Listing"
    const val MISLEADING_INFO = "Misleading Information"
    const val INAPPROPRIATE_CONTENT = "Inappropriate Content"
    const val SCAM = "Suspected Scam"
    const val DUPLICATE = "Duplicate Listing"
    const val WRONG_LOCATION = "Wrong Location"
    const val PRICE_MISMATCH = "Price Mismatch"
    const val OTHER = "Other"

    fun getAllReasons() = listOf(
        FAKE_LISTING,
        MISLEADING_INFO,
        INAPPROPRIATE_CONTENT,
        SCAM,
        DUPLICATE,
        WRONG_LOCATION,
        PRICE_MISMATCH,
        OTHER
    )
}