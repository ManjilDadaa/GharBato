package com.example.gharbato.repository

import com.example.gharbato.model.ReportUser

interface ReportUserRepo {
    fun reportUser(report: ReportUser, callback: (Boolean, String) -> Unit)
}
