package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.model.SupportMessage
import com.example.gharbato.model.SupportConversation
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSupportScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var conversations by remember { mutableStateOf<List<SupportConversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val database = FirebaseDatabase.getInstance()
    val messagesRef = database.getReference("support_messages")

    // Themed colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Load all conversations
    LaunchedEffect(Unit) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversationList = mutableListOf<SupportConversation>()

                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val messages = mutableListOf<SupportMessage>()

                    userSnapshot.children.forEach { messageSnapshot ->
                        val message = messageSnapshot.getValue(SupportMessage::class.java)
                        message?.let { messages.add(it) }
                    }

                    if (messages.isNotEmpty()) {
                        val sortedMessages = messages.sortedByDescending { it.timestamp }
                        val lastMessage = sortedMessages.first()
                        val unreadCount = messages.count { !it.isAdmin && it.timestamp > 0 }

                        conversationList.add(
                            SupportConversation(
                                userId = userId,
                                userName = lastMessage.senderName.ifEmpty { "User" },
                                userEmail = lastMessage.senderEmail,
                                userPhone = lastMessage.senderPhone,
                                userImage = lastMessage.senderImage,
                                lastMessage = lastMessage.message,
                                lastMessageTime = lastMessage.timestamp,
                                unreadCount = 0 // You can implement read/unread tracking
                            )
                        )
                    }
                }

                conversations = conversationList.sortedByDescending { it.lastMessageTime }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header
        Surface(
            color = surfaceColor,
            tonalElevation = if (isDarkMode) 0.dp else 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Support Messages",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Text(
                    text = "${conversations.size} conversation${if (conversations.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor.copy(alpha = 0.6f)
                    )
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        } else if (conversations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_support_agent_24),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Blue.copy(alpha = 0.5f)
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
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isDarkMode = isDarkMode,
                        onClick = {
                            // Navigate to admin chat screen with this user
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
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: SupportConversation,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with user image support
        if (conversation.userImage.isNotEmpty()) {
            AsyncImage(
                model = conversation.userImage,
                contentDescription = conversation.userName,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Blue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Blue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatConversationTime(conversation.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = textColor.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.7f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (conversation.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
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