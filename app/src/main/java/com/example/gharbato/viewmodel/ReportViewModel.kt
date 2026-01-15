package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.ReportedProperty
import com.example.gharbato.repository.ReportPropertyRepo
import com.example.gharbato.repository.UserRepoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ReportViewModel"

data class ReportUiState(
    val reportedProperties: List<ReportedProperty> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val reportSubmitted: Boolean = false
)

class ReportViewModel(
    private val repository: ReportPropertyRepo,
    private val userRepo: UserRepoImpl = UserRepoImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadReportedProperties()
    }

    private fun loadReportedProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getReportedProperties().collect { reports ->
                    _uiState.value = _uiState.value.copy(
                        reportedProperties = reports,
                        isLoading = false,
                        error = null
                    )
                    Log.d(TAG, "Loaded ${reports.size} reported properties")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reported properties: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load reported properties"
                )
            }
        }
    }

    fun submitReport(report: ReportedProperty) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, reportSubmitted = false)
            try {
                val result = repository.reportProperty(report)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Report submitted successfully",
                        reportSubmitted = true
                    )
                    Log.d(TAG, "Report submitted successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to submit report"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting report: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to submit report"
                )
            }
        }
    }

    fun deleteReportedProperty(reportId: String, propertyId: Int, ownerId: String, propertyTitle: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.deleteReportedProperty(reportId, propertyId)

                if (result.isSuccess) {
                    // Notify the property owner that their property was removed
                    userRepo.createNotification(
                        userId = ownerId,
                        title = "⚠️ Property Removed",
                        message = "Your property '$propertyTitle' has been removed due to reports violating our community guidelines.",
                        type = "system",
                        imageUrl = "",
                        actionData = ""
                    ) { _, _ -> }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Property removed successfully"
                    )
                    Log.d(TAG, "Property deleted successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete property"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting property: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete property"
                )
            }
        }
    }

    fun keepProperty(reportId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.dismissReport(reportId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Report dismissed, property kept"
                    )
                    Log.d(TAG, "Report dismissed successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to dismiss report"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing report: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to dismiss report"
                )
            }
        }
    }

    fun getReportCount(propertyId: Int, callback: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.getReportCount(propertyId)
                callback(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting report count: ${e.message}")
                callback(0)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null,
            reportSubmitted = false
        )
    }
}