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
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val usersRef = database.getReference("Users")

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Custom Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = if (isDarkMode) 0.dp else 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Support Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Blue, Blue.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Support Center",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLoading) "Loading..." else "${conversations.size} active conversation${if (conversations.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }

        // Divider
        HorizontalDivider(
            color = textColor.copy(alpha = 0.08f),
            thickness = 1.dp
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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
                            text = "Loading conversations...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                conversations.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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
                            text = "No Support Requests",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "When users reach out for help,\ntheir messages will appear here",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            items = conversations,
                            key = { it.userId }
                        ) { conversation ->
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
}

@Composable
fun ConversationCard(
    conversation: SupportConversation,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardColor = if (isDarkMode) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar with online indicator
            Box {
                if (conversation.userImage.isNotEmpty()) {
                    AsyncImage(
                        model = conversation.userImage,
                        contentDescription = conversation.userName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Blue.copy(alpha = 0.2f),
                                        Blue.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Blue,
                                fontSize = 22.sp
                            )
                        )
                    }
                }

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Conversation Details
            Column(modifier = Modifier.weight(1f)) {
                // User Name and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.userName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
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

                // Last Message Preview
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor.copy(alpha = 0.65f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Contact Info Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                                tint = Blue.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (conversation.userEmail.length > 18)
                                    conversation.userEmail.take(18) + "..."
                                else conversation.userEmail,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.55f)
                                ),
                                maxLines = 1
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
                                tint = Blue.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = conversation.userPhone,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.55f)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arrow Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Blue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatConversationTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
