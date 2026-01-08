package com.example.gharbato.repository

import android.util.Log
import com.example.gharbato.data.model.PropertyStatus
import com.example.gharbato.model.ReportStatus
import com.example.gharbato.model.ReportedProperty
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReportPropertyRepoImpl : ReportPropertyRepo {
    private val TAG = "ReportPropertyRepo"

    private val reportsRef = FirebaseDatabase.getInstance().getReference("ReportedProperties")
    private val propertiesRef = FirebaseDatabase.getInstance().getReference("Property")

    override suspend fun reportProperty(report: ReportedProperty): Result<Boolean> {
        return try {
            // Generate a unique report ID if not provided
            val reportId = if (report.reportId.isEmpty()) {
                reportsRef.push().key ?: return Result.failure(Exception("Failed to generate report ID"))
            } else {
                report.reportId
            }

            val reportWithId = report.copy(reportId = reportId)

            // Save the report
            reportsRef.child(reportId).setValue(reportWithId).await()

            Log.d(TAG, "Successfully reported property ${report.propertyId}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting property: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getReportedProperties(): Flow<List<ReportedProperty>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { dataSnapshot ->
                    try {
                        dataSnapshot.getValue(ReportedProperty::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing report: ${e.message}")
                        null
                    }
                }.filter { it.status == ReportStatus.PENDING }

                // Group by propertyId and take the most recent report
                val groupedReports = reports.groupBy { it.propertyId }
                val uniqueReports = groupedReports.map { (_, reportList) ->
                    reportList.maxByOrNull { it.reportedAt } ?: reportList.first()
                }

                trySend(uniqueReports.sortedByDescending { it.reportedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read reports.", error.toException())
                close(error.toException())
            }
        }

        reportsRef.addValueEventListener(listener)
        awaitClose { reportsRef.removeEventListener(listener) }
    }

    override suspend fun getReportsForProperty(propertyId: Int): List<ReportedProperty> {
        return try {
            val snapshot = reportsRef
                .orderByChild("propertyId")
                .equalTo(propertyId.toDouble())
                .get()
                .await()

            snapshot.children.mapNotNull { it.getValue(ReportedProperty::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reports for property $propertyId: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteReportedProperty(reportId: String, propertyId: Int): Result<Boolean> {
        return try {
            // Mark report as reviewed
            reportsRef.child(reportId)
                .child("status")
                .setValue(ReportStatus.REVIEWED)
                .await()

            // Delete the property from Property table
            val propertyQuery = propertiesRef.orderByChild("id").equalTo(propertyId.toDouble())
            val snapshot = propertyQuery.get().await()

            if (snapshot.exists()) {
                snapshot.children.forEach { dataSnapshot ->
                    // You can either delete or mark as REJECTED
                    dataSnapshot.ref.child("status").setValue(PropertyStatus.REJECTED).await()
                    // Or completely remove: dataSnapshot.ref.removeValue().await()
                }
                Log.d(TAG, "Successfully deleted property $propertyId")
                Result.success(true)
            } else {
                Result.failure(Exception("Property not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property $propertyId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun dismissReport(reportId: String): Result<Boolean> {
        return try {
            // Mark report as dismissed (keeping the property)
            reportsRef.child(reportId)
                .child("status")
                .setValue(ReportStatus.DISMISSED)
                .await()

            Log.d(TAG, "Successfully dismissed report $reportId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing report: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getReportCount(propertyId: Int): Int {
        return try {
            val snapshot = reportsRef
                .orderByChild("propertyId")
                .equalTo(propertyId.toDouble())
                .get()
                .await()

            snapshot.children.count {
                val report = it.getValue(ReportedProperty::class.java)
                report?.status == ReportStatus.PENDING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting report count: ${e.message}")
            0
        }
    }
}