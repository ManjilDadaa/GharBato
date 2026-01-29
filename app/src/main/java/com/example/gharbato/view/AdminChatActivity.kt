// File: app/src/main/java/com/example/gharbato/view/AdminChatActivity.kt
package com.example.gharbato.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
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
    var isUserOnline by remember { mutableStateOf(false) }

    // Initialize Firebase Database
    val database = FirebaseDatabase.getInstance("https://gharbatodb-default-rtdb.firebaseio.com")
    val messagesRef = database.getReference("support_messages").child(userId)
    val usersRef = database.getReference("Users")
    val userPresenceRef = database.getReference("user_presence").child(userId)

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

    // Listen to user online status and mark messages as delivered when user comes online
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            userPresenceRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val online = snapshot.child("online").getValue(Boolean::class.java) ?: false
                    isUserOnline = online

                    // When user comes online, mark all undelivered admin messages as delivered
                    if (online) {
                        messagesRef.get().addOnSuccessListener { messagesSnapshot ->
                            messagesSnapshot.children.forEach { messageData ->
                                // Check both "admin" (old format) and "isAdmin" field names
                                val isAdminMsg = messageData.child("admin").getValue(Boolean::class.java)
                                    ?: messageData.child("isAdmin").getValue(Boolean::class.java)
                                    ?: false
                                val isDelivered = messageData.child("isDelivered").getValue(Boolean::class.java) ?: false

                                // Mark admin messages as delivered when user comes online
                                if (isAdminMsg && !isDelivered) {
                                    messageData.ref.child("isDelivered").setValue(true)
                                }
                            }
                        }
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Custom Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shadowElevation = if (isDarkMode) 0.dp else 4.dp
        ) {
            Column {
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

                    // User profile image with online indicator
                    Box {
                        if (displayImage.isNotEmpty()) {
                            AsyncImage(
                                model = displayImage,
                                contentDescription = displayName,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Blue.copy(alpha = 0.3f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Blue.copy(alpha = 0.2f), Blue.copy(alpha = 0.1f))
                                        )
                                    )
                                    .border(2.dp, Blue.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Blue
                                    )
                                )
                            }
                        }

                        // Online indicator
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (isDarkMode) surfaceColor else Color.White)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isUserOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // User info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isUserOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            )
                            Text(
                                text = if (isUserOnline) "Online" else "Offline",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = if (isUserOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                                )
                            )
                        }
                    }
                }

                // Contact info row
                if (displayEmail.isNotEmpty() || displayPhone.isNotEmpty()) {
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (displayEmail.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Blue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = displayEmail,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor.copy(alpha = 0.7f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (displayPhone.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Blue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = displayPhone,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor.copy(alpha = 0.7f)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
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
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading conversation...",
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
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Blue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💬",
                                style = TextStyle(fontSize = 44.sp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Start Conversation",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Send a message to help\n$displayName with their inquiry",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Center
                        )
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
                            AdminMessageBubble(
                                message = message,
                                userName = displayName,
                                isDarkMode = isDarkMode,
                                isUserOnline = isUserOnline
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Bar
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
                            "Reply to $displayName...",
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

                // Send Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
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
                                        admin = true,
                                        isDelivered = false,
                                        isRead = false
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
fun AdminMessageBubble(
    message: SupportMessage,
    userName: String,
    isDarkMode: Boolean,
    isUserOnline: Boolean = false
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    // Admin messages come from senderId = "admin" OR isAdmin = true
    val isAdminMessage = message.isAdmin || message.senderId == "admin"

    // Define tick colors
    val greyTickColor = Color(0xFF9E9E9E)
    val blueTickColor = Blue

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAdminMessage) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isAdminMessage) 18.dp else 4.dp,
                bottomEnd = if (isAdminMessage) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdminMessage)
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
                // Show sender name for user messages only
                if (!isAdminMessage) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Blue
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isAdminMessage) Color.White else textColor,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Timestamp and read receipt
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatMessageTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            )

            // Read receipt indicators for admin messages only
            // Single grey tick (✓) = Sent (message not yet delivered/seen)
            // Double grey tick (✓✓) = Delivered (user is online but hasn't read)
            // Double blue tick (✓✓) = Read (user has seen the message)
            if (isAdminMessage) {
                when {
                    message.isRead -> {
                        // Double blue tick - Message has been read
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            modifier = Modifier.size(16.dp),
                            tint = blueTickColor
                        )
                    }
                    message.isDelivered || isUserOnline -> {
                        // Double grey tick - Message delivered or user is online
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            modifier = Modifier.size(16.dp),
                            tint = greyTickColor
                        )
                    }
                    else -> {
                        // Single grey tick - Message sent but not delivered
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Sent",
                            modifier = Modifier.size(16.dp),
                            tint = greyTickColor
                        )
                    }
                }
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
