package com.example.gharbato

import android.app.Application

class GharBatoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start listening for call invitations
        com.example.gharbato.view.CallInvitationManager.startListening(this)
    }
}
