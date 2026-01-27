package com.example.gharbato.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AdminChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: "User"

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminChatScreen(
                        userId = userId,
                        userName = userName
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen(
    userId: String,
    userName: String
) {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.getReference("support_messages").child(userId)

    val listState = rememberLazyListState()

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Load messages from Firebase
    LaunchedEffect(Unit) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedMessages = mutableListOf<SupportMessage>()
                snapshot.children.forEach { data ->
                    val message = data.getValue(SupportMessage::class.java)
                    message?.let { loadedMessages.add(it) }
                }
                messages = loadedMessages.sortedBy { it.timestamp }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = userName,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = textColor
                            )
                        )
                        Text(
                            text = "Support Chat",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            (context as? ComponentActivity)?.finish()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = if (isDarkMode) 0.dp else 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp, max = 120.dp),
                        placeholder = {
                            Text(
                                "Type your message...",
                                color = textColor.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue,
                            unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = Blue
                        ),
                        shape = RoundedCornerShape(28.dp),
                        maxLines = 4,
                        enabled = !isSending
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isSending) {
                                isSending = true
                                val messageId = messagesRef.push().key ?: return@IconButton
                                val newMessage = SupportMessage(
                                    id = messageId,
                                    senderId = "admin",
                                    senderName = "Admin Support",
                                    senderEmail = "",
                                    senderPhone = "",
                                    senderImage = "",
                                    message = messageText.trim(),
                                    timestamp = System.currentTimeMillis(),
                                    isAdmin = true
                                )

                                messagesRef.child(messageId).setValue(newMessage)
                                    .addOnSuccessListener {
                                        messageText = ""
                                        isSending = false
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                        isSending = false
                                    }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(
                                elevation = if (isDarkMode) 0.dp else 4.dp,
                                shape = RoundedCornerShape(28.dp)
                            )
                            .background(
                                color = if (messageText.isBlank() || isSending) Blue.copy(alpha = 0.5f) else Blue,
                                shape = RoundedCornerShape(28.dp)
                            ),
                        enabled = messageText.isNotBlank() && !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Blue
                )
            } else if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💬",
                        style = TextStyle(fontSize = 64.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to start the conversation",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        AdminMessageBubble(
                            message = message,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMessageBubble(
    message: SupportMessage,
    isDarkMode: Boolean
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val isAdminMessage = message.isAdmin

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAdminMessage) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAdminMessage) 16.dp else 4.dp,
                bottomEnd = if (isAdminMessage) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdminMessage) Blue else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDarkMode) 0.dp else 2.dp
            )
        ) {
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isAdminMessage) Color.White else textColor
                ),
                modifier = Modifier.padding(12.dp)
            )
        }

        Text(
            text = formatMessageTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
        )
    }
}

private fun formatMessageTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}