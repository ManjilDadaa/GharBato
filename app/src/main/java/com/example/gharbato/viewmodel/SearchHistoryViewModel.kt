package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.model.SearchHistory
import com.example.gharbato.repository.SearchHistoryRepo
import com.example.gharbato.repository.SearchHistoryRepoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



private const val TAG = "SearchHistoryVM"

data class SearchHistoryUiState(
    val searchHistory: List<SearchHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showClearAllDialog: Boolean = false
)

class SearchHistoryViewModel(
    private val repository: SearchHistoryRepo = SearchHistoryRepoImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchHistoryUiState())
    val uiState: StateFlow<SearchHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSearchHistory()
    }


    fun loadSearchHistory(limit: Int = 20) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getSearchHistory(limit) { success, history, message ->
                if (success && history != null) {
                    Log.d(TAG, "Loaded ${history.size} search history entries")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = history,
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to load search history: $message")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = emptyList(),
                        isLoading = false,
                        error = message ?: "Failed to load search history"
                    )
                }
            }
        }
    }


    fun saveTextSearch(
        query: String,
        resultsCount: Int,
        filters: Map<String, String> = emptyMap()
    ) {
        if (query.isBlank()) return

        viewModelScope.launch {
            val searchHistory = SearchHistory(
                searchQuery = query,
                searchType = SearchHistory.TYPE_TEXT,
                resultsCount = resultsCount,
                filters = filters,
                timestamp = System.currentTimeMillis()
            )

            repository.saveSearchHistory(searchHistory) { success, message ->
                if (success) {
                    Log.d(TAG, "Text search saved: $query")
                    // Reload to show updated list
                    loadSearchHistory()
                } else {
                    Log.e(TAG, "Failed to save text search: $message")
                }
            }
        }
    }


    fun saveLocationSearch(
        latitude: Double,
        longitude: Double,
        address: String,
        radius: Float,
        resultsCount: Int,
        filters: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val searchHistory = SearchHistory(
                searchQuery = "", // No text query for location searches
                searchType = SearchHistory.TYPE_LOCATION,
                locationLat = latitude,
                locationLng = longitude,
                locationAddress = address,
                locationRadius = radius,
                resultsCount = resultsCount,
                filters = filters,
                timestamp = System.currentTimeMillis()
            )

            repository.saveSearchHistory(searchHistory) { success, message ->
                if (success) {
                    Log.d(TAG, "Location search saved: $address")
                    // Reload to show updated list
                    loadSearchHistory()
                } else {
                    Log.e(TAG, "Failed to save location search: $message")
                }
            }
        }
    }


    fun loadHistoryByType(searchType: String, limit: Int = 20) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getSearchHistoryByType(searchType, limit) { success, history, message ->
                if (success && history != null) {
                    Log.d(TAG, "Loaded ${history.size} search history entries of type: $searchType")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = history,
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to load search history by type: $message")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = emptyList(),
                        isLoading = false,
                        error = message ?: "Failed to load search history"
                    )
                }
            }
        }
    }


    fun searchInHistory(query: String) {
        if (query.isBlank()) {
            loadSearchHistory()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.searchInHistory(query) { success, history, message ->
                if (success && history != null) {
                    Log.d(TAG, "Found ${history.size} matching search history entries")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = history,
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to search in history: $message")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = emptyList(),
                        isLoading = false,
                        error = message ?: "Search failed"
                    )
                }
            }
        }
    }


    fun deleteSearchHistory(historyId: String) {
        viewModelScope.launch {
            repository.deleteSearchHistory(historyId) { success, message ->
                if (success) {
                    Log.d(TAG, "Search history deleted: $historyId")
                    // Remove from UI state immediately
                    val updatedHistory = _uiState.value.searchHistory.filter { it.id != historyId }
                    _uiState.value = _uiState.value.copy(searchHistory = updatedHistory)
                } else {
                    Log.e(TAG, "Failed to delete search history: $message")
                    _uiState.value = _uiState.value.copy(
                        error = message ?: "Failed to delete entry"
                    )
                }
            }
        }
    }


    fun showClearAllDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDialog = true)
    }


    fun hideClearAllDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDialog = false)
    }

    /**
     * Clear all search history for current user
     */
    fun clearAllSearchHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showClearAllDialog = false)

            repository.clearAllSearchHistory { success, message ->
                if (success) {
                    Log.d(TAG, "All search history cleared")
                    _uiState.value = _uiState.value.copy(
                        searchHistory = emptyList(),
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to clear search history: $message")
                    _uiState.value = _uiState.value.copy(
                        error = message ?: "Failed to clear history"
                    )
                }
            }
        }
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }


    fun isUserAuthenticated(): Boolean {
        return repository.getCurrentUserId() != null
    }
}