package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState(initial = false)

    var conversations by remember { mutableStateOf<List<SupportConversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

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
                        val unreadCount = messages.count { !it.isAdmin && it.timestamp > getLastReadTimestamp(userId) }

                        conversationList.add(
                            SupportConversation(
                                userId = userId,
                                userName = lastMessage.senderName.ifEmpty { "User" },
                                userEmail = lastMessage.senderEmail,
                                userPhone = lastMessage.senderPhone,
                                userImage = lastMessage.senderImage,
                                lastMessage = lastMessage.message,
                                lastMessageTime = lastMessage.timestamp,
                                unreadCount = unreadCount
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

    // Filter conversations based on search and filter
    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conversation ->
            val matchesSearch = searchQuery.isEmpty() ||
                    conversation.userName.contains(searchQuery, ignoreCase = true) ||
                    conversation.userEmail.contains(searchQuery, ignoreCase = true) ||
                    conversation.lastMessage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Unread" -> conversation.unreadCount > 0
                "All" -> true
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = if (isDarkMode) 0.dp else 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Support Center",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "${conversations.size} active conversation${if (conversations.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Unread count badge
                        val totalUnread = conversations.sumOf { it.unreadCount }
                        if (totalUnread > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Blue,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "$totalUnread new",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search conversations...",
                                color = textColor.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_search_24),
                                contentDescription = "Search",
                                tint = textColor.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_clear_24),
                                        contentDescription = "Clear",
                                        tint = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = Blue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "All",
                            onClick = { selectedFilter = "All" },
                            label = {
                                Text(
                                    "All (${conversations.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == "Unread",
                            onClick = { selectedFilter = "Unread" },
                            label = {
                                Text(
                                    "Unread (${conversations.count { it.unreadCount > 0 }})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue,
                                selectedLabelColor = Color.White
                            )
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Blue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading conversations...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f)
                        )
                    )
                }
            } else if (filteredConversations.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_support_agent_24),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Blue.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Results Found" else "No Support Messages",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "Try adjusting your search terms"
                        else
                            "User support messages will appear here",
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
                    items(
                        items = filteredConversations,
                        key = { it.userId }
                    ) { conversation ->
                        ConversationItem(
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
                        if (conversation != filteredConversations.last()) {
                            HorizontalDivider(
                                color = textColor.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
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
    val hasUnread = conversation.unreadCount > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (hasUnread && !isDarkMode) {
            Blue.copy(alpha = 0.05f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
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
                                color = Blue
                            )
                        )
                    }
                }

                // Online status indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.userName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (conversation.userEmail.isNotEmpty()) {
                            Text(
                                text = conversation.userEmail,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.5f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = formatConversationTime(conversation.lastMessageTime),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (hasUnread) Blue else textColor.copy(alpha = 0.5f),
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = if (hasUnread) 0.9f else 0.6f),
                            fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Blue,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
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
        diff < 86400000 -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < 172800000 -> "Yesterday"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

// Helper function to track last read time (you can implement with SharedPreferences or Firebase)
private fun getLastReadTimestamp(userId: String): Long {
    // TODO: Implement proper read tracking with SharedPreferences or Firebase
    return 0L
}