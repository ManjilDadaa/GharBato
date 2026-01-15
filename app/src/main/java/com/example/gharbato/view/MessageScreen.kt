package com.example.gharbato.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MessageScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(messageViewModel: MessageViewModel = viewModel()) {
    val searchText by messageViewModel.searchText
    val users by messageViewModel.users
    val isLoading by messageViewModel.isLoading
    val errorMessage by messageViewModel.errorMessage
    val currentUser by messageViewModel.currentUser
    val chatNavigation by messageViewModel.chatNavigation
    val chatPreviews by messageViewModel.chatPreviews
    val aiAssistant by messageViewModel.aiAssistant
    val context = LocalContext.current
    val activity = context as Activity

    LaunchedEffect(Unit) {
        messageViewModel.loadUsers()
    }

    LaunchedEffect(searchText) {
        if (!isLoading) {
            messageViewModel.searchUsers()
        }
    }

    LaunchedEffect(chatNavigation) {
        val nav = chatNavigation ?: return@LaunchedEffect

        // Check if it's AI chat
        if (nav.isAiChat) {
            activity.startActivity(GeminiChatActivity.newIntent(activity))
        } else {
            activity.startActivity(
                MessageDetailsActivity.newIntent(
                    activity = activity,
                    otherUserId = nav.targetUserId,
                    otherUserName = nav.targetUserName,
                    otherUserImage = nav.targetUserImage
                )
            )
        }
        messageViewModel.onChatNavigationHandled()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Text(
                text = "Messages",
                color = Blue,
                fontSize = 32.sp,
                fontWeight = FontWeight.W500,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { messageViewModel.onSearchTextChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(28.dp)),
                placeholder = {
                    Text(
                        "",
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE0B0FF))
                }
            } else if (errorMessage.isNotEmpty() && users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Still show AI Assistant even when no other chats
                        val aiPreview = messageViewModel.getAiChatPreview(context)
                        AiChatListItem(
                            lastMessage = aiPreview.lastMessageText,
                            time = if (aiPreview.lastMessageTime > 0) formatChatTime(aiPreview.lastMessageTime) else "",
                            onClick = {
                                messageViewModel.requestChatNavigation(
                                    aiAssistant.userId,
                                    aiAssistant.fullName,
                                    ""
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = errorMessage,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // AI Assistant - Always at the top (unless searching and doesn't match)
                    if (searchText.isEmpty() ||
                        aiAssistant.fullName.lowercase().contains(searchText.lowercase()) ||
                        "ai".contains(searchText.lowercase())) {
                        item {
                            val aiPreview = messageViewModel.getAiChatPreview(context)
                            AiChatListItem(
                                lastMessage = aiPreview.lastMessageText,
                                time = if (aiPreview.lastMessageTime > 0) formatChatTime(aiPreview.lastMessageTime) else "",
                                onClick = {
                                    messageViewModel.requestChatNavigation(
                                        aiAssistant.userId,
                                        aiAssistant.fullName,
                                        ""
                                    )
                                }
                            )
                        }
                    }

                    // Regular users
                    items(users) { user ->
                        val displayName = user.fullName.ifBlank {
                            if (user.email.isNotBlank()) user.email.substringBefore("@") else "User"
                        }
                        val preview = chatPreviews[user.userId]
                        val lastMessage = preview?.lastMessageText ?: ""
                        val timeText = preview?.let { formatChatTime(it.lastMessageTime) } ?: ""
                        val statusText = ""
                        ChatListItem(
                            name = displayName,
                            message = if (lastMessage.isNotEmpty()) lastMessage else "Tap to start chatting",
                            time = if (timeText.isNotEmpty()) timeText else "",
                            imageUrl = user.profileImageUrl,
                            status = statusText,
                            onClick = {
                                messageViewModel.requestChatNavigation(user.userId, displayName, user.profileImageUrl)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiChatListItem(
    lastMessage: String,
    time: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Avatar with gradient
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667EEA),
                                Color(0xFF764BA2)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Assistant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // AI Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF667EEA)
                    ) {
                        Text(
                            text = "AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMessage,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Time
            if (time.isNotEmpty()) {
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Top)
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    name: String,
    message: String,
    time: String,
    imageUrl: String,
    status: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (status.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Blue,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // Time
        Text(
            text = time,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Top)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageScreen() {
    MessageScreen()
}

private fun formatChatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getChatStatus(lastMessageTime: Long): String {
    if (lastMessageTime <= 0L) return ""
    val diff = System.currentTimeMillis() - lastMessageTime
    return if (diff < 2 * 60 * 1000) {
        "Online"
    } else {
        "Last online ${getTimeAgo(lastMessageTime)}"
    }
}
