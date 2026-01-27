package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.model.SupportConversation
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var conversations by remember { mutableStateOf<List<SupportConversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.getReference("support_messages")
    val usersRef = database.getReference("users")

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Load all conversations
    LaunchedEffect(Unit) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversationList = mutableListOf<SupportConversation>()
                val userIds = mutableListOf<String>()

                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    userIds.add(userId)
                    val messages = mutableListOf<SupportMessage>()

                    userSnapshot.children.forEach { messageSnapshot ->
                        val message = messageSnapshot.getValue(SupportMessage::class.java)
                        message?.let { messages.add(it) }
                    }

                    if (messages.isNotEmpty()) {
                        // Get user info from the first non-admin message
                        val userMessage = messages.firstOrNull { !it.isAdmin }

                        val sortedMessages = messages.sortedByDescending { it.timestamp }
                        val lastMessage = sortedMessages.first()

                        conversationList.add(
                            SupportConversation(
                                userId = userId,
                                userName = userMessage?.senderName ?: "",
                                userEmail = userMessage?.senderEmail ?: "",
                                userPhone = userMessage?.senderPhone ?: "",
                                userImage = userMessage?.senderImage ?: "",
                                lastMessage = lastMessage.message,
                                lastMessageTime = lastMessage.timestamp,
                                unreadCount = 0
                            )
                        )
                    }
                }

                // First set conversations to show loading is done
                conversations = conversationList.sortedByDescending { it.lastMessageTime }
                isLoading = false

                // Then fetch user profiles from users node to get full names and images
                conversationList.forEachIndexed { index, conversation ->
                    usersRef.child(conversation.userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val fullName = userSnapshot.child("fullName").getValue(String::class.java) ?: ""
                            val userName = userSnapshot.child("userName").getValue(String::class.java) ?: ""
                            val profileImage = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            val phone = userSnapshot.child("phoneNo").getValue(String::class.java) ?: ""

                            // Use fullName first, then userName, then fallback to "User"
                            val displayName = when {
                                fullName.isNotEmpty() -> fullName
                                userName.isNotEmpty() -> userName
                                else -> "User"
                            }

                            // Update the conversation with user data from users node
                            conversations = conversations.map { conv ->
                                if (conv.userId == conversation.userId) {
                                    conv.copy(
                                        userName = displayName,
                                        userImage = profileImage.ifEmpty { conv.userImage },
                                        userEmail = email.ifEmpty { conv.userEmail },
                                        userPhone = phone.ifEmpty { conv.userPhone }
                                    )
                                } else conv
                            }.sortedByDescending { it.lastMessageTime }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Support Messages",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Text(
                            text = "${conversations.size} conversation${if (conversations.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Support Messages",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "User support messages will appear here",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(conversations) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            isDarkMode = isDarkMode,
                            onClick = {
                                val intent = Intent(context, AdminChatActivity::class.java).apply {
                                    putExtra("userId", conversation.userId)
                                    putExtra("userName", conversation.userName)
                                    putExtra("userEmail", conversation.userEmail)
                                    putExtra("userPhone", conversation.userPhone)
                                    putExtra("userImage", conversation.userImage)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: SupportConversation,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box {
                if (conversation.userImage.isNotEmpty()) {
                    AsyncImage(
                        model = conversation.userImage,
                        contentDescription = conversation.userName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Blue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Blue,
                                fontSize = 20.sp
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
                        .background(Color(0xFF4CAF50))
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Conversation Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // User Name
                Text(
                    text = conversation.userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Last Message Preview
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor.copy(alpha = 0.7f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Contact Info Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (conversation.userEmail.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Blue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = conversation.userEmail.take(20) + if (conversation.userEmail.length > 20) "..." else "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (conversation.userPhone.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Blue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = conversation.userPhone,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side Info
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Timestamp
                Text(
                    text = formatConversationTime(conversation.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = textColor.copy(alpha = 0.5f)
                    )
                )

                // Unread Badge
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Blue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Arrow Icon
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatConversationTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}