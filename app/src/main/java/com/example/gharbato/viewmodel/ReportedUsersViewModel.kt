package com.example.gharbato.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gharbato.model.ReportUser
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.ReportUserRepo
import com.example.gharbato.repository.UserRepo
import java.text.SimpleDateFormat
import java.util.*

data class ReportedUser(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userImage: String,
    val reportCount: Int,
    val reportReason: String,
    val accountStatus: String
)

class ReportedUsersViewModel(
    private val reportRepo: ReportUserRepo,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _reportedUsers = MutableLiveData<List<ReportedUser>>()
    val reportedUsers: LiveData<List<ReportedUser>> get() = _reportedUsers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadReportedUsers() {
        _isLoading.value = true
        reportRepo.getReportedUsers { reports ->
            val reportsMap = reports.groupBy { it.reportedUserId }
            val reportedUsersList = mutableListOf<ReportedUser>()
            var processedCount = 0
            val totalUsersToFetch = reportsMap.size

            if (totalUsersToFetch == 0) {
                _reportedUsers.value = emptyList()
                _isLoading.value = false
                return@getReportedUsers
            }

            reportsMap.forEach { (userId, userReports) ->
                userRepo.getUser(userId) { userModel ->
                    val userName = userModel?.fullName ?: "Unknown User"
                    val userEmail = userModel?.email ?: "No Email"
                    val userImage = userModel?.profileImageUrl ?: ""
                    val latestReport = userReports.maxByOrNull { it.timestamp }
                    val reason = latestReport?.reason ?: "No reason provided"
                    val isSuspended = userModel?.isSuspended ?: false
                    val accountStatus = if (isSuspended) "Suspended" else "Active"

                    reportedUsersList.add(
                        ReportedUser(
                            userId = userId,
                            userName = userName,
                            userEmail = userEmail,
                            userImage = userImage,
                            reportCount = userReports.size,
                            reportReason = reason,
                            accountStatus = accountStatus
                        )
                    )

                    processedCount++
                    if (processedCount == totalUsersToFetch) {
                        _reportedUsers.value = reportedUsersList
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun suspendUser(userId: String, duration: Long, reason: String, callback: (Boolean, String) -> Unit) {
        reportRepo.suspendUser(userId, duration, reason) { success, message ->
            if (success) loadReportedUsers()
            callback(success, message)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}