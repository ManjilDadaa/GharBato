package com.example.gharbato.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
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
    var isSending by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: ""
    // Use explicit Firebase URL to ensure consistency with AdminChatActivity
    val database = FirebaseDatabase.getInstance("https://gharbatodb-default-rtdb.firebaseio.com")
    val messagesRef = database.getReference("support_messages").child(currentUserId)
    val usersRef = database.getReference("users").child(currentUserId)
    val userPresenceRef = database.getReference("user_presence").child(currentUserId)

    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Send button animation
    val sendButtonScale by animateFloatAsState(
        targetValue = if (messageText.isNotBlank() && !isSending) 1f else 0.9f,
        label = "sendButtonScale"
    )

    // Set user online presence when entering/leaving the chat
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (currentUserId.isNotEmpty()) {
                        userPresenceRef.child("online").setValue(true)
                        userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                        userPresenceRef.child("online").onDisconnect().setValue(false)
                        userPresenceRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (currentUserId.isNotEmpty()) {
                        userPresenceRef.child("online").setValue(false)
                        userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (currentUserId.isNotEmpty()) {
                userPresenceRef.child("online").setValue(false)
                userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
            }
        }
    }

    // Load user profile
    LaunchedEffect(Unit) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userProfile = snapshot.value as? Map<String, Any>
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Load messages from Firebase and mark admin messages as read
    LaunchedEffect(Unit) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedMessages = mutableListOf<SupportMessage>()
                snapshot.children.forEach { data ->
                    val message = data.getValue(SupportMessage::class.java)
                    message?.let {
                        loadedMessages.add(it)

                        // Mark admin messages as read and delivered when user views them
                        if (it.isAdmin && (!it.isRead || !it.isDelivered)) {
                            val updates = hashMapOf<String, Any>(
                                "isRead" to true,
                                "isDelivered" to true
                            )
                            data.ref.updateChildren(updates)
                        }
                    }
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
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Enhanced Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shadowElevation = if (isDarkMode) 0.dp else 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = { (context as? ComponentActivity)?.finish() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Support Avatar with gradient
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Blue, Blue.copy(alpha = 0.7f))
                            )
                        )
                        .border(2.dp, Blue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GharBato Support",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "Usually replies within minutes",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }

        // Chat Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(backgroundColor)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Blue,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading messages...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Support Icon with background
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Blue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = Blue,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "How can we help?",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Send us a message and our support team will get back to you as soon as possible.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f),
                                lineHeight = 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick action hints
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode)
                                    surfaceColor
                                else
                                    Blue.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "You can ask about:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                QuickHelpItem("Account & Profile issues")
                                QuickHelpItem("Property listing questions")
                                QuickHelpItem("Payment & Transactions")
                                QuickHelpItem("App features & how to use")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
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

        // Enhanced Bottom Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp, max = 120.dp),
                    placeholder = {
                        Text(
                            "Type your message...",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = Blue,
                        focusedContainerColor = if (isDarkMode) Color.Transparent else Color(0xFFF8F9FA),
                        unfocusedContainerColor = if (isDarkMode) Color.Transparent else Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(26.dp),
                    maxLines = 4,
                    enabled = !isSending,
                    textStyle = TextStyle(fontSize = 15.sp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Send Button with animation
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .scale(sendButtonScale)
                        .shadow(
                            elevation = if (messageText.isBlank() || isSending) 0.dp else 6.dp,
                            shape = CircleShape,
                            ambientColor = Blue.copy(alpha = 0.3f),
                            spotColor = Blue.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = if (messageText.isBlank() || isSending)
                                Brush.linearGradient(listOf(Blue.copy(alpha = 0.4f), Blue.copy(alpha = 0.3f)))
                            else
                                Brush.linearGradient(listOf(Blue, Blue.copy(alpha = 0.85f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null && !isSending) {
                                isSending = true
                                val messageId = messagesRef.push().key

                                if (messageId != null) {
                                    val newMessage = SupportMessage(
                                        id = messageId,
                                        senderId = currentUser.uid,
                                        senderName = userProfile?.get("fullName") as? String ?: currentUser.displayName ?: "User",
                                        senderEmail = userProfile?.get("email") as? String ?: currentUser.email ?: "",
                                        senderPhone = userProfile?.get("phoneNo") as? String ?: "",
                                        senderImage = userProfile?.get("profileImageUrl") as? String ?: "",
                                        message = messageText.trim(),
                                        timestamp = System.currentTimeMillis(),
                                        admin = false
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
                                } else {
                                    Toast.makeText(context, "Failed to generate message ID", Toast.LENGTH_SHORT).show()
                                    isSending = false
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickHelpItem(text: String) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Blue)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textColor.copy(alpha = 0.8f)
            )
        )
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
        // Message bubble
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMyMessage) 18.dp else 4.dp,
                bottomEnd = if (isMyMessage) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMyMessage)
                    Blue
                else if (isDarkMode)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    Color(0xFFF0F2F5)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDarkMode) 0.dp else 1.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Show admin label for admin messages
                if (!isMyMessage && message.isAdmin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Support Team",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Blue
                            )
                        )
                    }
                }

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isMyMessage) Color.White else textColor,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Timestamp
        Text(
            text = formatMessageTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor.copy(alpha = 0.45f),
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
        )
    }
}

private fun formatMessageTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> {
            val minutes = diff / 60000
            "$minutes min ago"
        }
        diff < 86400000 -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "Today ${sdf.format(Date(timestamp))}"
        }
        diff < 172800000 -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "Yesterday ${sdf.format(Date(timestamp))}"
        }
        else -> {
            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
