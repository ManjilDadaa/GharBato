package com.example.gharbato

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.view.ThemePreference
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class GharBatoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appCheckProviderFactory = if (BuildConfig.DEBUG) {
            val debugFactoryClass = Class.forName(
                "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
            )
            debugFactoryClass.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(appCheckProviderFactory)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                SystemBarUtils.setSystemBarsAppearance(
                    activity,
                    ThemePreference.getDarkModeSync(activity)
                )
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        // Start listening for call invitations
        com.example.gharbato.view.CallInvitationManager.startListening(this)
    }
}
