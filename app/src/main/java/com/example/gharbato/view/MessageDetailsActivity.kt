package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

class MessageDetailsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MessageDetailsActivity"
        private const val EXTRA_OTHER_USER_ID = "other_user_id"
        private const val EXTRA_OTHER_USER_NAME = "other_user_name"
        private const val EXTRA_OTHER_USER_IMAGE = "other_user_image"

        fun newIntent(
            activity: Activity,
            otherUserId: String,
            otherUserName: String,
            otherUserImage: String = ""
        ): Intent {
            return Intent(activity, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(EXTRA_OTHER_USER_NAME, otherUserName)
                putExtra(EXTRA_OTHER_USER_IMAGE, otherUserImage)
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: ""
        val otherUserImage = intent.getStringExtra(EXTRA_OTHER_USER_IMAGE) ?: ""

        val currentUserId = auth.currentUser?.uid ?: ""

        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            Log.e(TAG, "User IDs are empty! Current: $currentUserId, Other: $otherUserId")
            finish()
            return
        }

        // Generate consistent chat ID
        val chatId = getChatId(currentUserId, otherUserId)

        Log.d(TAG, "=== Chat Setup ===")
        Log.d(TAG, "Current User ID: $currentUserId")
        Log.d(TAG, "Other User ID: $otherUserId")
        Log.d(TAG, "Other User Name: $otherUserName")
        Log.d(TAG, "Chat ID: $chatId")

        setContent {
            MessageDetailsScreen(
                currentUserId = currentUserId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserImage = otherUserImage,
                chatId = chatId,
                onBackClick = { finish() }
            )
        }
    }

    /**
     * Generate consistent chat ID by sorting user IDs
     * This ensures the same chat ID is generated regardless of who initiates the chat
     */
    private fun getChatId(userId1: String, userId2: String): String {
        val sortedIds = listOf(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailsScreen(
    currentUserId: String,
    otherUserId: String,
    otherUserName: String,
    otherUserImage: String,
    chatId: String,
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    val listState = rememberLazyListState()

    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.reference
        .child("chats")
        .child(chatId)
        .child("messages")

    // Listen for messages
    LaunchedEffect(chatId) {
        Log.d("MessageDetails", "Setting up listener for chat: $chatId")

        val messageListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<Message>()

                snapshot.children.forEach { messageSnapshot ->
                    try {
                        val messageId = messageSnapshot.key ?: ""
                        val senderId = messageSnapshot.child("senderId").getValue(String::class.java) ?: ""
                        val receiverId = messageSnapshot.child("receiverId").getValue(String::class.java) ?: ""
                        val messageContent = messageSnapshot.child("message").getValue(String::class.java) ?: ""
                        val timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val isRead = messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                        val message = Message(
                            messageId = messageId,
                            senderId = senderId,
                            receiverId = receiverId,
                            message = messageContent,
                            timestamp = timestamp,
                            isRead = isRead
                        )

                        messageList.add(message)
                    } catch (e: Exception) {
                        Log.e("MessageDetails", "Error parsing message", e)
                    }
                }

                // Sort by timestamp
                messages = messageList.sortedBy { it.timestamp }

                Log.d("MessageDetails", "Loaded ${messages.size} messages for chat: $chatId")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MessageDetails", "Failed to load messages: ${error.message}")
            }
        }

        messagesRef.addValueEventListener(messageListener)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                userName = otherUserName,
                userImage = otherUserImage,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId
                    )
                }
            }

            // Message Input
            MessageInput(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        sendMessage(
                            chatId = chatId,
                            currentUserId = currentUserId,
                            otherUserId = otherUserId,
                            messageText = messageText,
                            messagesRef = messagesRef
                        )
                        messageText = ""
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    userName: String,
    userImage: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (userImage.isNotEmpty()) {
                        AsyncImage(
                            model = userImage,
                            contentDescription = userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Column {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Blue else Color.White,
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(message.timestamp),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Blue,
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5)
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Blue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

private fun sendMessage(
    chatId: String,
    currentUserId: String,
    otherUserId: String,
    messageText: String,
    messagesRef: DatabaseReference
) {
    val messageId = messagesRef.push().key ?: return

    val message = hashMapOf(
        "senderId" to currentUserId,
        "receiverId" to otherUserId,
        "message" to messageText,
        "timestamp" to ServerValue.TIMESTAMP,
        "isRead" to false
    )

    Log.d("MessageDetails", "Sending message to chat: $chatId")
    Log.d("MessageDetails", "From: $currentUserId, To: $otherUserId")

    messagesRef
        .child(messageId)
        .setValue(message)
        .addOnSuccessListener {
            Log.d("MessageDetails", "Message sent successfully")
        }
        .addOnFailureListener { e ->
            Log.e("MessageDetails", "Failed to send message", e)
        }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val now = Calendar.getInstance()

    return when {
        isSameDay(calendar, now) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        }
        isYesterday(calendar, now) -> {
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(calendar.time)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val yesterday = cal2.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, yesterday)
}