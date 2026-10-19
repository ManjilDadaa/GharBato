package com.example.gharbato.utils

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import com.example.gharbato.ui.theme.GharBatoPalette

object SystemBarUtils {
    fun setSystemBarsAppearance(activity: Activity, isDarkMode: Boolean) {
        val backgroundColor = (
            if (isDarkMode) GharBatoPalette.backgroundDark else GharBatoPalette.backgroundLight
        ).toArgb()
        activity.window.statusBarColor = backgroundColor
        activity.window.navigationBarColor = backgroundColor
        activity.window.decorView.setBackgroundColor(backgroundColor)

        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }
}
