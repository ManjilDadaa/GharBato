package com.example.gharbato.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FloatingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_CALL
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: ""
        val isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, true)
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val currentUserId = intent.getStringExtra(EXTRA_CURRENT_USER_ID) ?: ""
        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: ""
        val senderId = intent.getStringExtra(EXTRA_SENDER_ID) ?: ""
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: ""
        val messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT) ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0x80000000)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (type == TYPE_CALL) {
                            CallFloatingCard(
                                callerName = callerName,
                                isVideoCall = isVideoCall,
                                callId = callId,
                                currentUserId = currentUserId,
                                otherUserId = otherUserId,
                                onDismiss = { finish() }
                            )
                        } else {
                            MessageFloatingCard(
                                senderName = senderName,
                                messageText = messageText,
                                senderId = senderId,
                                onDismiss = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun acceptCall(callId: String, currentUserId: String, otherUserId: String, isVideoCall: Boolean) {
        val context = this
        if (callId.isEmpty() || currentUserId.isEmpty() || otherUserId.isEmpty()) {
            finish()
            return
        }

        CallInvitationManager.setCurrentCall(callId)

        val intent = ZegoCallActivity.newIntent(
            activity = this,
            callId = callId,
            userId = currentUserId,
            userName = "Me",
            isVideoCall = isVideoCall,
            targetUserId = otherUserId,
            isIncomingCall = true
        )

        FirebaseDatabase.getInstance().getReference("call_invitations").child(currentUserId).removeValue()
        FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()

        startActivity(intent)
        finish()
    }

    private fun declineCall(currentUserId: String) {
        if (currentUserId.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("call_invitations").child(currentUserId).removeValue()
            FirebaseDatabase.getInstance().getReference("call_invitations").child("demo_user").removeValue()
        }
        CallInvitationManager.endCall()
        finish()
    }

    private fun openMessage(senderId: String, senderName: String) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null || senderId.isEmpty()) {
            finish()
            return
        }

        val intent = MessageDetailsActivity.newIntent(
            activity = this,
            otherUserId = senderId,
            otherUserName = senderName
        )
        startActivity(intent)
        finish()
    }

    @Composable
    private fun CallFloatingCard(
        callerName: String,
        isVideoCall: Boolean,
        callId: String,
        currentUserId: String,
        otherUserId: String,
        onDismiss: () -> Unit
    ) {
        val displayName = if (callerName.isNotBlank()) callerName else "Incoming call"
        val context = LocalContext.current as FloatingCallActivity

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                    Column {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVideoCall) "Incoming video call" else "Incoming audio call",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { context.declineCall(currentUserId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Decline")
                    }

                    Button(
                        onClick = { context.acceptCall(callId, currentUserId, otherUserId, isVideoCall) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Accept")
                    }
                }

                Text(
                    text = "Tap outside to dismiss",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onDismiss() }
                )
            }
        }
    }

    @Composable
    private fun MessageFloatingCard(
        senderName: String,
        messageText: String,
        senderId: String,
        onDismiss: () -> Unit
    ) {
        val displayName = if (senderName.isNotBlank()) senderName else "New message"
        val displayText = if (messageText.isNotBlank()) messageText else "Tap to open chat"
        val context = LocalContext.current as FloatingCallActivity

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1F5FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = Color(0xFF0288D1)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayText,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View",
                        fontSize = 13.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.clickable {
                            context.openMessage(senderId, displayName)
                        }
                    )
                    Text(
                        text = "Dismiss",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALLER_NAME = "caller_name"
        const val EXTRA_IS_VIDEO_CALL = "is_video_call"
        const val EXTRA_CURRENT_USER_ID = "current_user_id"
        const val EXTRA_OTHER_USER_ID = "other_user_id"

        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_SENDER_ID = "sender_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_MESSAGE_TEXT = "message_text"

        const val TYPE_CALL = "call"
        const val TYPE_MESSAGE = "message"
    }
}
