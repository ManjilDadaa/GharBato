package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.gharbato.utils.ZegoCloudConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ZegoCallActivity : FragmentActivity() {

    private var callId: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("ZegoCall", "onCreate started")

        callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        val isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, true)
        val targetUserId = intent.getStringExtra(EXTRA_TARGET_USER_ID) ?: ""
        val isIncomingCall = intent.getBooleanExtra(EXTRA_IS_INCOMING_CALL, false)

        android.util.Log.d("ZegoCall", "Intent data: callId=$callId, userId=$userId, userName=$userName, isVideo=$isVideoCall, target=$targetUserId, incoming=$isIncomingCall")

        // Mark this call as active
        CallInvitationManager.setCurrentCall(callId)

        if (ZegoCloudConstants.APP_ID == 0L || ZegoCloudConstants.APP_SIGN.isBlank()) {
            android.util.Log.e("ZegoCall", "ZEGOCLOUD missing. Detected appId=${ZegoCloudConstants.APP_ID}, appSignLen=${ZegoCloudConstants.APP_SIGN.length}. Set ZEGO_APP_ID/ZEGO_APP_SIGN in local.properties and sync.")
            Toast.makeText(
                this,
                "Calling service not available. Please contact support.",
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

        // Remove emulator check for development - allow testing on emulator
        // Note: ZegoCloud may have limited functionality on emulator, but won't crash
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true)

        val hasX86Abi = Build.SUPPORTED_ABIS.any { it.startsWith("x86") }
        if (isEmulator || hasX86Abi) {
            // Show warning but don't crash - allow for testing
            android.util.Log.w("ZegoCall", "Running on emulator/x86. ZEGOCLOUD calls may have limited functionality.")
            Toast.makeText(this, "Emulator detected. If video is black, enable 'Hardware - GLES 2.0' in AVD Settings.", Toast.LENGTH_LONG).show()
        }

        // Set up the container first
        val containerId = View.generateViewId()
        val container = FrameLayout(this).apply {
            id = containerId
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        // Initialize call immediately without delay
        try {
            initializeCall(containerId, callId, userId, userName, isVideoCall, targetUserId, isIncomingCall)
        } catch (e: Exception) {
            android.util.Log.e("ZegoCall", "Failed to initialize call", e)
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the active call when activity is destroyed
        CallInvitationManager.endCall()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val allGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                // Retry initialization after permissions granted
                recreate()
            } else {
                Toast.makeText(this, "Permissions required for call", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeCall(
        containerId: Int,
        callId: String,
        userId: String,
        userName: String,
        isVideoCall: Boolean,
        targetUserId: String,
        isIncomingCall: Boolean
    ) {
        try {
            // Check and request permissions first with better error handling
            if (isVideoCall) {
                // For video calls, we need camera and microphone
                if (!hasCameraAndMicPermissions()) {
                    requestPermissions()
                    return
                }
            } else {
                // For voice calls, we need microphone only
                if (!hasMicPermission()) {
                    requestPermissions()
                    return
                }
            }

            // Add try-catch around ZegoCloud initialization
            val config = try {
                if (isVideoCall) {
                    ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
                        turnOnCameraWhenJoining = true
                        turnOnMicrophoneWhenJoining = true
                        useSpeakerWhenJoining = true
                    }
                } else {
                    // Use VideoCall config even for audio calls to allow video toggling/viewing
                    ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
                        turnOnCameraWhenJoining = false
                        turnOnMicrophoneWhenJoining = true
                        useSpeakerWhenJoining = false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ZegoCall", "Failed to create call config", e)
                Toast.makeText(this, "Call configuration failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Create and add fragment with proper error handling
            try {
                android.util.Log.d("ZegoCall", "Creating ZegoCloud fragment with callId: $callId, userId: $userId")
                
                val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                    ZegoCloudConstants.APP_ID,
                    ZegoCloudConstants.APP_SIGN,
                    userId,
                    userName,
                    callId,
                    config
                )

                android.util.Log.d("ZegoCall", "Fragment created successfully")

                // Use commit() instead of commitNow() to allow proper fragment initialization
                supportFragmentManager.beginTransaction()
                    .replace(containerId, fragment)
                    .commit()

                android.util.Log.d("ZegoCall", "Fragment added to container")

                if (!isIncomingCall && targetUserId.isNotEmpty()) {
                    sendCallInvitation(targetUserId, callId, isVideoCall, userName)
                }
            } catch (e: Exception) {
                android.util.Log.e("ZegoCall", "Failed to create call fragment", e)
                Toast.makeText(this, "Failed to start call: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (t: Throwable) {
            Toast.makeText(this, "Failed to start call: ${t.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun hasCameraAndMicPermissions(): Boolean {
        return (checkSelfPermission(android.Manifest.permission.CAMERA) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    private fun hasMicPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(android.Manifest.permission.RECORD_AUDIO)
        if (intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, true)) {
            permissions.add(android.Manifest.permission.CAMERA)
        }
        requestPermissions(permissions.toTypedArray(), 100)
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
            android.util.Log.d("ZegoCall", "Sending call invitation: target=$targetUserId, callId=$callId, video=$isVideoCall, caller=$callerName")
            
            val database = FirebaseDatabase.getInstance()
            val callRef = database.getReference("call_invitations").child(targetUserId)

            val invitation = mapOf(
                "callId" to callId,
                "callerId" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                "callerName" to callerName,
                "isVideoCall" to isVideoCall,
                "timestamp" to System.currentTimeMillis()
            )

            android.util.Log.d("ZegoCall", "Invitation data: $invitation")

            callRef.setValue(invitation).addOnSuccessListener {
                android.util.Log.d("ZegoCall", "Call invitation sent successfully to Firebase")
            }.addOnFailureListener { error ->
                android.util.Log.e("ZegoCall", "Failed to send call invitation", error)
            }
        }
    }
}

data class IncomingCall(
    val callId: String,
    val callerId: String,
    val callerName: String,
    val isVideoCall: Boolean,
    val currentUserId: String
)

object CallInvitationManager {
    private var isListening = false
    private var pendingInvitation: Map<String, Any>? = null
    private var currentCallId: String? = null // Track ongoing call

    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()

    fun startListening(context: android.content.Context) {
        if (isListening) return
        isListening = true

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        
        // For demo: Listen to both current user and demo_user
        // This allows testing between any two users
        val callRef1 = database.getReference("call_invitations").child(currentUserId)
        val callRef2 = database.getReference("call_invitations").child("demo_user")
        
        android.util.Log.d("CallInvitation", "Listening for calls on: $currentUserId and demo_user")

        callRef1.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                try {
                    val invitation = snapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                    if (invitation != null) {
                        processInvitation(invitation, context, currentUserId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallInvitation", "Error processing invitation", e)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("CallInvitation", "Database error", error.toException())
            }
        })
        
        callRef2.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                try {
                    val invitation = snapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                    if (invitation != null) {
                        processInvitation(invitation, context, currentUserId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallInvitation", "Error processing invitation", e)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("CallInvitation", "Database error", error.toException())
            }
        })
    }
    
    private fun processInvitation(
        invitation: Map<String, Any>,
        context: android.content.Context,
        currentUserId: String
    ) {
        val callId = invitation["callId"] as? String ?: ""
        val callerId = invitation["callerId"] as? String ?: ""
        val callerName = invitation["callerName"] as? String ?: "Unknown"
        val isVideoCall = invitation["isVideoCall"] as? Boolean ?: true
        val timestamp = invitation["timestamp"] as? Long ?: 0L

        android.util.Log.d("CallInvitation", "Received invitation: callId=$callId, from=$callerId, video=$isVideoCall")

        // Check if invitation is recent (within 30 seconds)
        val currentTime = System.currentTimeMillis()
        if (currentTime - timestamp > 30000) {
            // Remove old invitation
            FirebaseDatabase.getInstance().getReference("call_invitations").child(currentUserId).removeValue()
            FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()
            return
        }

        // Check if already in a call
        if (currentCallId != null && currentCallId == callId) {
            // Already in this call, remove invitation
            FirebaseDatabase.getInstance().getReference("call_invitations").child(currentUserId).removeValue()
            FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()
            return
        }

        // Don't answer our own calls
        if (callerId == currentUserId) {
            return
        }

        pendingInvitation = invitation
        _incomingCall.value = IncomingCall(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            isVideoCall = isVideoCall,
            currentUserId = currentUserId
        )
    }

    fun acceptCurrentCall(context: android.content.Context) {
        val call = _incomingCall.value ?: return
        if (context !is android.app.Activity) {
            android.util.Log.e("CallInvitation", "Context is not an Activity")
            return
        }
        startIncomingCall(context, call.callId, call.currentUserId, call.callerId, call.isVideoCall)
        FirebaseDatabase.getInstance().getReference("call_invitations").child(call.currentUserId).removeValue()
        FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()
        _incomingCall.value = null
    }

    fun rejectCurrentCall() {
        val call = _incomingCall.value ?: return
        FirebaseDatabase.getInstance().getReference("call_invitations").child(call.currentUserId).removeValue()
        FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()
        _incomingCall.value = null
    }

    private fun startIncomingCall(
        context: android.content.Context,
        callId: String,
        currentUserId: String,
        callerId: String,
        isVideoCall: Boolean
    ) {
        try {
            // Check if context is valid
            if (context !is android.app.Activity) {
                android.util.Log.e("CallInvitation", "Context is not an Activity")
                return
            }
            
            currentCallId = callId // Mark this call as active
            android.util.Log.d("CallInvitation", "Starting incoming call with callId: $callId")
            
            val intent = android.content.Intent(context, ZegoCallActivity::class.java).apply {
                putExtra(ZegoCallActivity.EXTRA_CALL_ID, callId) // Use SAME call ID!
                putExtra(ZegoCallActivity.EXTRA_USER_ID, currentUserId)
                putExtra(ZegoCallActivity.EXTRA_USER_NAME, "Me")
                putExtra(ZegoCallActivity.EXTRA_IS_VIDEO_CALL, isVideoCall)
                putExtra(ZegoCallActivity.EXTRA_TARGET_USER_ID, callerId)
                putExtra(ZegoCallActivity.EXTRA_IS_INCOMING_CALL, true)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("CallInvitation", "Failed to start incoming call", e)
            currentCallId = null // Clear on error
        }
    }
    
    fun getPendingInvitation(): Map<String, Any>? {
        val invitation = pendingInvitation
        pendingInvitation = null
        return invitation
    }

    fun setCurrentCall(callId: String?) {
        currentCallId = callId
    }

    fun getCurrentCall(): String? = currentCallId

    fun endCall() {
        currentCallId = null
    }
}
