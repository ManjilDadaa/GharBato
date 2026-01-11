package com.example.gharbato.model

data class ReportUser(
    val reportId: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val timestamp: Long = 0L
)
