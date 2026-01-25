package com.example.gharbato.utils

import android.app.Activity
import androidx.core.view.WindowCompat

object SystemBarUtils {
    fun setSystemBarsAppearance(activity: Activity, isDarkMode: Boolean) {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }
}
