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
        activity.startActivity(
            MessageDetailsActivity.newIntent(
                activity = activity,
                otherUserId = nav.targetUserId,
                otherUserName = nav.targetUserName,
                otherUserImage = nav.targetUserImage
            )
        )
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
                color = Blue, // Light Purple color
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
                        "", // Empty placeholder as per design usually or icon
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
                    focusedContainerColor = Color.Transparent, // Using background modifier
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
                    Text(
                        text = errorMessage,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        val displayName = user.fullName.ifBlank { 
                            if (user.email.isNotBlank()) user.email.substringBefore("@") else "User" 
                        }
                        ChatListItem(
                            name = displayName,
                            message = "Tap to start chatting", // Placeholder as we don't have last message in UserModel
                            time = "Now", // Placeholder
                            imageUrl = user.profileImageUrl,
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
fun ChatListItem(
    name: String,
    message: String,
    time: String,
    imageUrl: String,
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
                // Initials or placeholder
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name and Message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // We can add an icon here if needed, e.g., for "Video call"
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Blue,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
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

