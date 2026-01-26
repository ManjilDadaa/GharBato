package com.example.gharbato.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
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
    var showUserInfo by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.getReference("support_messages").child(userId)

    val listState = rememberLazyListState()

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Load messages
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

    // Auto scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.clickable { showUserInfo = !showUserInfo }
                        ) {
                            if (userImage.isNotEmpty()) {
                                AsyncImage(
                                    model = userImage,
                                    contentDescription = userName,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Blue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Blue
                                        )
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = userName,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = if (showUserInfo) "Hide user info" else "Tap for user info",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = Blue
                                    )
                                )
                            }
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

                // User Info Card (collapsible)
                if (showUserInfo) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (userEmail.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_email_24),
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Email",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textColor
                                            )
                                        )
                                    }
                                }
                            }

                            if (userPhone.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_phone_24),
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Phone",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = userPhone,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textColor
                                            )
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "User ID: $userId",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
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
                                "Type your response...",
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
                            if (messageText.isNotBlank()) {
                                val messageId = messagesRef.push().key ?: return@IconButton
                                val newMessage = SupportMessage(
                                    id = messageId,
                                    senderId = "admin",
                                    senderName = "Admin",
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
                        text = "Start the conversation",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to help this user",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f)
                        )
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
                            userName = userName,
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
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender label
                Text(
                    text = if (isAdminMessage) "Admin" else (message.senderName.ifEmpty { userName }),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAdminMessage) Color.White.copy(alpha = 0.9f)
                        else textColor.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isAdminMessage) Color.White else textColor
                    )
                )
            }
        }

        Text(
            text = formatAdminMessageTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
        )
    }
}

private fun formatAdminMessageTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}