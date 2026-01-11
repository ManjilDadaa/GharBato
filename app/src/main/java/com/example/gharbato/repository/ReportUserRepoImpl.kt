package com.example.gharbato.repository

import com.example.gharbato.model.ReportUser
import com.google.firebase.database.FirebaseDatabase

class ReportUserRepoImpl : ReportUserRepo {
    private val database = FirebaseDatabase.getInstance()
    private val reportsRef = database.getReference("user_reports")

    override fun reportUser(report: ReportUser, callback: (Boolean, String) -> Unit) {
        val reportId = reportsRef.push().key ?: return
        val reportWithId = report.copy(reportId = reportId)
        
        reportsRef.child(reportId).setValue(reportWithId)
            .addOnSuccessListener {
                callback(true, "User reported successfully")
            }
            .addOnFailureListener { e ->
                callback(false, e.message ?: "Failed to report user")
            }
    }
}
