// File: app/src/main/java/com/example/gharbato/view/AdminChatActivity.kt
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class AdminChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: "User"
        val userEmail = intent.getStringExtra("userEmail") ?: ""
        val userPhone = intent.getStringExtra("userPhone") ?: ""
        val userImage = intent.getStringExtra("userImage") ?: ""

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
                        userName = userName,
                        userEmail = userEmail,
                        userPhone = userPhone,
                        userImage = userImage
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
    userName: String,
    userEmail: String,
    userPhone: String,
    userImage: String
) {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }

    // State for user info (fetch from Firebase if not provided)
    var displayName by remember { mutableStateOf(userName) }
    var displayEmail by remember { mutableStateOf(userEmail) }
    var displayPhone by remember { mutableStateOf(userPhone) }
    var displayImage by remember { mutableStateOf(userImage) }

    // Initialize Firebase Database
    val database = FirebaseDatabase.getInstance("https://gharbatodb-default-rtdb.firebaseio.com")
    val messagesRef = database.getReference("support_messages").child(userId)
    val usersRef = database.getReference("Users")

    val listState = rememberLazyListState()

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Fetch user profile from Firebase to get actual name
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    val fetchedUserName = snapshot.child("userName").getValue(String::class.java) ?: ""
                    val profileImage = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phoneNo").getValue(String::class.java) ?: ""

                    // Use fullName first, then userName, then fallback to passed userName or "User"
                    displayName = when {
                        fullName.isNotEmpty() -> fullName
                        fetchedUserName.isNotEmpty() -> fetchedUserName
                        userName.isNotEmpty() && userName != "User" -> userName
                        else -> "User"
                    }
                    if (profileImage.isNotEmpty()) displayImage = profileImage
                    if (email.isNotEmpty()) displayEmail = email
                    if (phone.isNotEmpty()) displayPhone = phone
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // Load messages from Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loadedMessages = mutableListOf<SupportMessage>()
                    snapshot.children.forEach { data ->
                        val message = data.getValue(SupportMessage::class.java)
                        message?.let {
                            loadedMessages.add(it)
                        }
                    }
                    messages = loadedMessages.sortedBy { it.timestamp }
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                    Toast.makeText(context, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            isLoading = false
            Toast.makeText(context, "Invalid user ID", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100) // Small delay to ensure layout is complete
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = if (isDarkMode) 0.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = {
                            (context as? ComponentActivity)?.finish()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }

                    // User profile image
                    if (displayImage.isNotEmpty()) {
                        AsyncImage(
                            model = displayImage,
                            contentDescription = displayName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Blue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Blue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // User info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayName,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayEmail.isNotEmpty()) {
                            Text(
                                text = displayEmail,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (displayPhone.isNotEmpty()) {
                            Text(
                                text = displayPhone,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Online status
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        },
        bottomBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = if (isDarkMode) 0.dp else 4.dp,
                shadowElevation = 8.dp
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
                                "Reply to $displayName...",
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
                                val messageId = messagesRef.push().key

                                if (messageId != null) {
                                    val newMessage = SupportMessage(
                                        id = messageId,
                                        senderId = "admin",
                                        senderName = "Support Team",
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
                                        .addOnFailureListener { exception ->
                                            Toast.makeText(
                                                context,
                                                "Failed to send: ${exception.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSending = false
                                        }
                                } else {
                                    Toast.makeText(context, "Failed to generate message ID", Toast.LENGTH_SHORT).show()
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
                                color = if (messageText.isBlank() || isSending)
                                    Blue.copy(alpha = 0.5f)
                                else
                                    Blue,
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
                                imageVector = Icons.AutoMirrored.Filled.Send,
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Blue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading conversation...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f)
                        )
                    )
                }
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
                        text = "Start Conversation",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to assist $displayName with their inquiry",
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        AdminMessageBubble(
                            message = message,
                            userName = displayName,
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
    userName: String,
    isDarkMode: Boolean
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    // Admin messages come from senderId = "admin" OR isAdmin = true
    val isAdminMessage = message.isAdmin || message.senderId == "admin"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAdminMessage) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAdminMessage) 16.dp else 4.dp,
                bottomEnd = if (isAdminMessage) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdminMessage)
                    Blue
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDarkMode) 0.dp else 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name for user messages only
                if (!isAdminMessage) {
                    Text(
                        text = userName,
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
                        color = if (isAdminMessage) Color.White else textColor
                    )
                )
            }
        }

        // Timestamp
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatMessageTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.5f)
                )
            )

            // Delivery indicator for admin messages
            if (isAdminMessage) {
                Text(
                    text = "✓✓",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Blue.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
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