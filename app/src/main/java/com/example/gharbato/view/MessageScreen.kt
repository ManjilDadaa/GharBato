package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun MessageScreen(messageViewModel: MessageViewModel = viewModel()) {
    val searchText by messageViewModel.searchText
    val users by messageViewModel.users
    val isLoading by messageViewModel.isLoading
    val errorMessage by messageViewModel.errorMessage
    val currentUser by messageViewModel.currentUser
    val context = LocalContext.current
    val activity = context as Activity
    
    // Load users on initial composition
    LaunchedEffect(Unit) {
        messageViewModel.loadUsers()
    }
    
    // Search users when search text changes
    LaunchedEffect(searchText) {
        if (!isLoading) {
            messageViewModel.searchUsers()
        }
    }
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Messages ",
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontSize = 27.sp,
                    fontWeight = FontWeight.W400
                )
            )

            Spacer(modifier = Modifier.height(15.dp))
            
            OutlinedTextField(
                value = searchText,
                onValueChange = { messageViewModel.onSearchTextChanged(it) },
                modifier = Modifier.fillMaxWidth()
                    .height(51.dp),
                placeholder = { 
                    Text(
                        "Search users...",
                        fontSize = 14.sp,
                        color = Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_search_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = Gray,
                    focusedContainerColor = Gray.copy(alpha = 0.15f),
                    unfocusedContainerColor = Gray.copy(alpha = 0.15f)
                ),
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading users...", color = Gray)
                }
            } else {
                if (errorMessage.isNotEmpty() && users.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: $errorMessage", color = Color.Red)
                    }
                }
                
                LazyColumn {
                    // "Me" entry for self-chat
                    item {
                        val myId = currentUser?.userId ?: messageViewModel.getLocalUserId(activity)
                        val myName = currentUser?.userName ?: "Me"
                        
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = "Me",
                            userId = myId,
                            userName = myName,
                            onMessageClick = {
                                messageViewModel.navigateToChat(myId, myName, activity)
                            },
                            onVideoCallClick = {
                                messageViewModel.initiateCall(myId, myName, true, activity)
                            },
                            onVoiceCallClick = {
                                messageViewModel.initiateCall(myId, myName, false, activity)
                            }
                        )
                    }
                    
                    // Firebase users
                    items(users) { user ->
                        val displayName = user.fullName.ifBlank { user.email }
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = displayName,
                            userId = user.userId,
                            userName = displayName,
                            onMessageClick = {
                                messageViewModel.navigateToChat(user.userId, displayName, activity)
                            },
                            onVideoCallClick = {
                                messageViewModel.initiateCall(user.userId, displayName, true, activity)
                            },
                            onVoiceCallClick = {
                                messageViewModel.initiateCall(user.userId, displayName, false, activity)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageUserItem(
    imageRes: Int,
    name: String,
    userId: String,
    userName: String,
    onMessageClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onVoiceCallClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onMessageClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hello, how are you?",
                fontSize = 14.sp,
                color = Gray
            )
        }
        
        // Call buttons
        Row {
            // Voice call button
            Icon(
                painter = painterResource(R.drawable.outline_call_24),
                contentDescription = "Voice Call",
                modifier = Modifier.size(24.dp)
                    .clickable(onClick = onVoiceCallClick)
                    .padding(4.dp),
                tint = Blue
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Video call button
            Icon(
                painter = painterResource(R.drawable.outline_videocam_24),
                contentDescription = "Video Call",
                modifier = Modifier.size(24.dp)
                    .clickable(onClick = onVideoCallClick)
                    .padding(4.dp),
                tint = Blue
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageScreen() {
    MessageScreen()
}

