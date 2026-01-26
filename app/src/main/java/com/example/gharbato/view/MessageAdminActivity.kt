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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MessageAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MessageAdminScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageAdminScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: ""
    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.getReference("support_messages").child(currentUserId)
    val usersRef = database.getReference("users").child(currentUserId)

    val listState = rememberLazyListState()

    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Load user profile
    LaunchedEffect(Unit) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userProfile = snapshot.value as? Map<String, Any>
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

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
                            text = "Admin Support",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = textColor
                            )
                        )
                        Text(
                            text = "We're here to help",
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
                            painter = painterResource(id = R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
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
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null && userProfile != null) {
                                val messageId = messagesRef.push().key ?: return@IconButton
                                val newMessage = SupportMessage(
                                    id = messageId,
                                    senderId = currentUser.uid,
                                    senderName = userProfile?.get("fullName") as? String ?: currentUser.displayName ?: "User",
                                    senderEmail = userProfile?.get("email") as? String ?: currentUser.email ?: "",
                                    senderPhone = userProfile?.get("phoneNo") as? String ?: "",
                                    senderImage = userProfile?.get("profileImageUrl") as? String ?: "",
                                    message = messageText.trim(),
                                    timestamp = System.currentTimeMillis(),
                                    isAdmin = false
                                )

                                messagesRef.child(messageId).setValue(newMessage)
                                    .addOnSuccessListener {
                                        messageText = ""
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
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
                                color = if (messageText.isBlank()) Blue.copy(alpha = 0.5f) else Blue,
                                shape = RoundedCornerShape(28.dp)
                            ),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_send_24),
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
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
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_support_agent_24),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Blue.copy(alpha = 0.5f)
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
                        text = "Send a message to start the conversation with our support team",
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
                        UserMessageBubble(
                            message = message,
                            currentUserId = currentUserId,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessageBubble(
    message: SupportMessage,
    currentUserId: String,
    isDarkMode: Boolean
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val isMyMessage = message.senderId == currentUserId && !message.isAdmin

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                bottomEnd = if (isMyMessage) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMyMessage) Blue else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDarkMode) 0.dp else 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name only for admin messages (incoming messages from admin)
                if (!isMyMessage && message.isAdmin) {
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isMyMessage) Color.White else textColor
                    )
                )
            }
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