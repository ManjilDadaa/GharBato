package com.example.gharbato.viewmodel

import com.example.gharbato.repository.UserRepoImpl

/**
 * Singleton to share the same UserViewModel instance across all screens
 * This ensures real-time updates work properly across the entire app
 */
object UserViewModelProvider {

    // Single instance shared across all screens
    private var instance: UserViewModel? = null

    fun getInstance(): UserViewModel {
        if (instance == null) {
            instance = UserViewModel(UserRepoImpl())
        }
        return instance!!
    }

    // Call this when user logs out to clear the instance
    fun clearInstance() {
        instance?.stopObservingNotifications()
        instance = null
    }
}