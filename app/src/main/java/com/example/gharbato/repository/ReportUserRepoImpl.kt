package com.example.gharbato.repository

import com.example.gharbato.model.ReportUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ReportUserRepoImpl : ReportUserRepo {
    private val database = FirebaseDatabase.getInstance()
    private val reportsRef = database.getReference("user_reports")
    private val usersRef = database.getReference("Users")

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
    
    override fun getReportedUsers(callback: (List<ReportUser>) -> Unit) {
        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { 
                    it.getValue(ReportUser::class.java) 
                }
                callback(reports)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }
    
    override fun suspendUser(userId: String, duration: Long, reason: String, callback: (Boolean, String) -> Unit) {
        val updates = mapOf(
            "isSuspended" to true,
            "suspendedUntil" to (System.currentTimeMillis() + duration),
            "suspensionReason" to reason
        )
        
        usersRef.child(userId).updateChildren(updates)
            .addOnSuccessListener { callback(true, "User suspended successfully") }
            .addOnFailureListener { callback(false, "Failed to suspend user") }
    }

    override fun activateUser(userId: String, callback: (Boolean, String) -> Unit) {
        val updates = mapOf(
            "isSuspended" to false,
            "suspendedUntil" to 0L,
            "suspensionReason" to ""
        )

        usersRef.child(userId).updateChildren(updates)
            .addOnSuccessListener { callback(true, "User activated successfully") }
            .addOnFailureListener { callback(false, "Failed to activate user") }
    }

    override fun resolveUser(userId: String, callback: (Boolean, String) -> Unit) {
        reportsRef.orderByChild("reportedUserId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        callback(true, "No reports found")
                        return
                    }

                    var deletedCount = 0
                    val total = snapshot.childrenCount

                    snapshot.children.forEach { 
                        it.ref.removeValue().addOnCompleteListener {
                            deletedCount++
                            if (deletedCount.toLong() == total) {
                                callback(true, "All reports resolved")
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, error.message)
                }
            })
    }
}
