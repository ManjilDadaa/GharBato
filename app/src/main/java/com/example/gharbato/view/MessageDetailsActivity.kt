package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gharbato.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MessageDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: "Chat"

        setContent {
            MessageDetailsScreen(
                otherUserId = otherUserId,
                otherUserName = otherUserName,
            )
        }
    }

    companion object {
        const val EXTRA_OTHER_USER_ID = "extra_other_user_id"
        const val EXTRA_OTHER_USER_NAME = "extra_other_user_name"

        fun newIntent(
            activity: Activity,
            otherUserId: String,
            otherUserName: String,
        ): Intent {
            return Intent(activity, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(EXTRA_OTHER_USER_NAME, otherUserName)
            }
        }
    }
}

private fun getOrCreateLocalUserId(context: Context): String {
    val prefs = context.getSharedPreferences("gharbato_prefs", Context.MODE_PRIVATE)
    val existing = prefs.getString("local_user_id", null)
    if (!existing.isNullOrBlank()) return existing

    val newId = "guest_${System.currentTimeMillis()}"
    prefs.edit().putString("local_user_id", newId).apply()
    return newId
}

private fun sanitizeZegoId(value: String): String {
    if (value.isBlank()) return "user"
    return value.replace(Regex("[^A-Za-z0-9_]"), "_")
}

private fun buildChatId(userA: String, userB: String): String {
    val a = sanitizeZegoId(userA)
    val b = sanitizeZegoId(userB)
    return if (a <= b) "${a}_$b" else "${b}_$a"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDetailsScreen(
    otherUserId: String,
    otherUserName: String,
) {
    val context = LocalContext.current
    val activity = context as Activity

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseDatabase.getInstance() }

    val myUserIdRaw = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
    val myUserId = remember(myUserIdRaw) { sanitizeZegoId(myUserIdRaw) }
    val otherId = remember(otherUserId) { sanitizeZegoId(otherUserId.ifBlank { "other" }) }

    val chatId = remember(myUserId, otherId) { buildChatId(myUserId, otherId) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        val query = db.getReference("chats")
            .child(chatId)
            .child("messages")
            .orderByChild("timestamp")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = mutableListOf<ChatMessage>()
                snapshot.children.forEach { child ->
                    val msg = child.getValue(ChatMessage::class.java) ?: return@forEach
                    val id = child.key ?: msg.id
                    newMessages.add(msg.copy(id = id))
                }
                messages.clear()
                messages.addAll(newMessages)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = otherUserName) },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            activity.startActivity(
                                ZegoCallActivity.newIntent(
                                    activity = activity,
                                    callId = chatId,
                                    userId = myUserId,
                                    userName = auth.currentUser?.email ?: myUserId,
                                    isVideoCall = false,
                                    targetUserId = otherId
                                )
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    }

                    IconButton(
                        onClick = {
                            activity.startActivity(
                                ZegoCallActivity.newIntent(
                                    activity = activity,
                                    callId = chatId,
                                    userId = myUserId,
                                    userName = auth.currentUser?.email ?: myUserId,
                                    isVideoCall = true,
                                    targetUserId = otherId
                                )
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == myUserId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = if (isMe) TextAlign.End else TextAlign.Start,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = if (isMe) TextAlign.End else TextAlign.Start,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(0.dp))

                IconButton(
                    onClick = {
                        val text = messageText.trim()
                        if (text.isEmpty()) return@IconButton

                        val ref = db.getReference("chats")
                            .child(chatId)
                            .child("messages")
                            .push()

                        val message = ChatMessage(
                            id = ref.key ?: "",
                            senderId = myUserId,
                            senderName = auth.currentUser?.email ?: myUserId,
                            text = text,
                            timestamp = System.currentTimeMillis(),
                        )

                        ref.setValue(message)
                        messageText = ""
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}
