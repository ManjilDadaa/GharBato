package com.example.gharbato.repository

import com.example.gharbato.model.SearchHistory


interface SearchHistoryRepo {


    fun saveSearchHistory(
        searchHistory: SearchHistory,
        callback: (success: Boolean, message: String?) -> Unit
    )


    fun getSearchHistory(
        limit: Int = 20,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    )


    fun getSearchHistoryByType(
        searchType: String,
        limit: Int = 20,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    )


    fun searchInHistory(
        query: String,
        callback: (success: Boolean, history: List<SearchHistory>?, message: String?) -> Unit
    )


    fun deleteSearchHistory(
        historyId: String,
        callback: (success: Boolean, message: String?) -> Unit
    )


    fun clearAllSearchHistory(
        callback: (success: Boolean, message: String?) -> Unit
    )


    fun getCurrentUserId(): String?
}