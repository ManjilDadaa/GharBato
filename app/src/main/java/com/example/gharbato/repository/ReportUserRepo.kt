package com.example.gharbato.repository

import com.example.gharbato.model.ReportUser

interface ReportUserRepo {
    fun reportUser(report: ReportUser, callback: (Boolean, String) -> Unit)
    fun getReportedUsers(callback: (List<ReportUser>) -> Unit)
    fun suspendUser(userId: String, duration: Long, reason: String, callback: (Boolean, String) -> Unit)
}
