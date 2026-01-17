package com.example.gharbato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gharbato.repository.SearchHistoryRepo
import com.example.gharbato.repository.SearchHistoryRepoImpl


class SearchHistoryViewModelFactory(
    private val repository: SearchHistoryRepo = SearchHistoryRepoImpl()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchHistoryViewModel::class.java)) {
            return SearchHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}