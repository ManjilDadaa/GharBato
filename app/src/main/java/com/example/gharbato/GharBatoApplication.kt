package com.example.gharbato

import android.app.Application
import com.example.gharbato.utils.NotificationHelper

class GharBatoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.gharbato.view.CallInvitationManager.startListening(this)
        NotificationHelper.startListeningForMessages(this)
    }
}
