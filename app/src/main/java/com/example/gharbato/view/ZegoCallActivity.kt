package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.gharbato.zego.ZegoCloudConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class ZegoCallActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        val isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, true)
        val targetUserId = intent.getStringExtra(EXTRA_TARGET_USER_ID) ?: ""
        val isIncomingCall = intent.getBooleanExtra(EXTRA_IS_INCOMING_CALL, false)

        if (ZegoCloudConstants.APP_ID == 0L || ZegoCloudConstants.APP_SIGN.isBlank()) {
            Toast.makeText(
                this,
                "ZEGOCLOUD missing. Detected appId=${ZegoCloudConstants.APP_ID}, appSignLen=${ZegoCloudConstants.APP_SIGN.length}. Set ZEGO_APP_ID/ZEGO_APP_SIGN in local.properties and sync.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        if (callId.isBlank() || userId.isBlank() || userName.isBlank()) {
            Toast.makeText(this, "Missing call parameters", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true)

        val hasX86Abi = Build.SUPPORTED_ABIS.any { it.startsWith("x86") }
        if (isEmulator || hasX86Abi) {
            Toast.makeText(
                this,
                "ZEGOCLOUD calls may not work on emulator/x86. Use a real device.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val containerId = View.generateViewId()
        val container = FrameLayout(this).apply {
            id = containerId
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        try {
            val config = if (isVideoCall) {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
            } else {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
            }

            val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                ZegoCloudConstants.APP_ID,
                ZegoCloudConstants.APP_SIGN,
                userId,
                userName,
                callId,
                config
            )

            supportFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .commitNow()

            // Send call invitation if this is an outgoing call
            if (!isIncomingCall && targetUserId.isNotEmpty()) {
                sendCallInvitation(targetUserId, callId, isVideoCall, userName)
            }
        } catch (t: Throwable) {
            Toast.makeText(this, "Failed to start call: ${t.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_IS_VIDEO_CALL = "extra_is_video_call"
        const val EXTRA_TARGET_USER_ID = "extra_target_user_id"
        const val EXTRA_IS_INCOMING_CALL = "extra_is_incoming_call"

        fun newIntent(
            activity: Activity,
            callId: String,
            userId: String,
            userName: String,
            isVideoCall: Boolean = true,
            targetUserId: String = "",
            isIncomingCall: Boolean = false
        ): Intent {
            return Intent(activity, ZegoCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_IS_VIDEO_CALL, isVideoCall)
                putExtra(EXTRA_TARGET_USER_ID, targetUserId)
                putExtra(EXTRA_IS_INCOMING_CALL, isIncomingCall)
            }
        }

        private fun sendCallInvitation(
            targetUserId: String,
            callId: String,
            isVideoCall: Boolean,
            callerName: String
        ) {
            val database = FirebaseDatabase.getInstance()
            val callRef = database.getReference("call_invitations").child(targetUserId)

            val invitation = mapOf(
                "callId" to callId,
                "callerId" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                "callerName" to callerName,
                "isVideoCall" to isVideoCall,
                "timestamp" to System.currentTimeMillis()
            )

            callRef.setValue(invitation)
        }
    }
}

// Call invitation listener to be added to Application class
object CallInvitationManager {
    private var isListening = false
    private var pendingInvitation: Map<String, Any>? = null

    fun startListening(context: android.content.Context) {
        if (isListening) return
        isListening = true

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val callRef = database.getReference("call_invitations").child(currentUserId)

        callRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val invitation = snapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                if (invitation != null) {
                    val callId = invitation["callId"] as? String ?: ""
                    val callerId = invitation["callerId"] as? String ?: ""
                    val callerName = invitation["callerName"] as? String ?: "Unknown"
                    val isVideoCall = invitation["isVideoCall"] as? Boolean ?: true

                    // Remove the invitation after processing
                    callRef.removeValue()

                    // Store the invitation for later processing
                    pendingInvitation = invitation
                    
                    // Try to start the call activity with a proper intent
                    try {
                        val intent = android.content.Intent(context, ZegoCallActivity::class.java).apply {
                            putExtra(ZegoCallActivity.EXTRA_CALL_ID, callId)
                            putExtra(ZegoCallActivity.EXTRA_USER_ID, currentUserId)
                            putExtra(ZegoCallActivity.EXTRA_USER_NAME, "Me")
                            putExtra(ZegoCallActivity.EXTRA_IS_VIDEO_CALL, isVideoCall)
                            putExtra(ZegoCallActivity.EXTRA_TARGET_USER_ID, callerId)
                            putExtra(ZegoCallActivity.EXTRA_IS_INCOMING_CALL, true)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where context cannot start activity
                        pendingInvitation = null
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle error
            }
        })
    }
    
    fun getPendingInvitation(): Map<String, Any>? {
        val invitation = pendingInvitation
        pendingInvitation = null
        return invitation
    }
}
