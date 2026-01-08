package com.example.gharbato.repository

import com.example.gharbato.data.model.ReportedProperty
import kotlinx.coroutines.flow.Flow

interface ReportPropertyRepo {
    /**
     * Submit a report for a property
     */
    suspend fun reportProperty(report: ReportedProperty): Result<Boolean>

    /**
     * Get all reported properties as a Flow
     */
    fun getReportedProperties(): Flow<List<ReportedProperty>>

    /**
     * Get reports for a specific property
     */
    suspend fun getReportsForProperty(propertyId: Int): List<ReportedProperty>

    /**
     * Delete a reported property (removes the property from listings)
     */
    suspend fun deleteReportedProperty(reportId: String, propertyId: Int): Result<Boolean>

    /**
     * Keep the reported property (mark report as dismissed)
     */
    suspend fun dismissReport(reportId: String): Result<Boolean>

    /**
     * Get report count for a property
     */
    suspend fun getReportCount(propertyId: Int): Int
}