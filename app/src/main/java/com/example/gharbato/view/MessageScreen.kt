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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.UserViewModel


class MessageScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MessageScreen()
        }
    }
}

private fun getOrCreateLocalUserId(context: Context): String {
    val prefs = context.getSharedPreferences("gharbato_prefs", Context.MODE_PRIVATE)
    val existing = prefs.getString("local_user_id", null)
    if (!existing.isNullOrBlank()) return existing

    val newId = "guest_${System.currentTimeMillis()}"
    prefs.edit().putString("local_user_id", newId).apply()
    return newId
}

data class FirebaseUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = ""
)

@Composable
fun MessageScreen(){
    var searchText by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as Activity
    
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    
    // Load only registered users from database
    LaunchedEffect(Unit) {
        android.util.Log.d("MessageScreen", "Loading registered users from database...")
        
        userViewModel.getAllUsers { success, userList, message ->
            android.util.Log.d("MessageScreen", "Database fetch completed - success: $success, userList size: ${userList?.size}")
            
            // Always set loading to false
            isLoading = false
            
            if (success && userList != null) {
                users = userList
                if (userList.isEmpty()) {
                    errorMessage = "No registered users found in database"
                } else {
                    errorMessage = "Found ${userList.size} registered users"
                }
                android.util.Log.d("MessageScreen", "Loaded ${userList.size} registered users: ${userList.map { it.fullName }}")
            } else {
                users = emptyList()
                errorMessage = "Error loading users: $message"
                android.util.Log.e("MessageScreen", "Failed to load users: $message")
            }
        }
    }
    
    // Search users when query changes
    LaunchedEffect(searchText) {
        if (isLoading) return@LaunchedEffect
        android.util.Log.d("MessageScreen", "Searching users with query: '$searchText'")
        
        try {
            kotlinx.coroutines.withTimeout(3000) {
                userViewModel.searchUsers(searchText) { success, userList, message ->
                    if (success && userList != null) {
                        android.util.Log.d("MessageScreen", "Search returned ${userList.size} users: ${userList.map { it.fullName }}")
                        users = userList
                        errorMessage = ""
                    } else {
                        android.util.Log.e("MessageScreen", "Search error: $message")
                        errorMessage = message
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("MessageScreen", "Search timeout, keeping current users")
            errorMessage = "Search timeout"
        }
    }
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(text = "Messages ",
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 27.sp,
                    fontWeight = FontWeight.W400))

            Spacer(modifier = Modifier.height(15.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it},
                modifier = Modifier.fillMaxWidth()
                    .height(51.dp),
                placeholder = { Text("Search users...",
                    fontSize = 14.sp,
                    color = Gray)},
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_search_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },singleLine = true,
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
                // Always show user list when not loading
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
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val myId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
                        val myName = auth.currentUser?.email ?: "Me"
                        
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = "Me",
                            userId = myId,
                            userName = myName,
                            onMessageClick = {
                                activity.startActivity(
                                    MessageDetailsActivity.newIntent(
                                        activity = activity,
                                        otherUserId = myId,
                                        otherUserName = myName,
                                    )
                                )
                            },
                            onVideoCallClick = {
                                startVideoCall(activity, myId, myName)
                            },
                            onVoiceCallClick = {
                                startVoiceCall(activity, myId, myName)
                            }
                        )
                    }
                    
                    // Firebase users
                    items(users) { user ->
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = user.fullName.ifBlank { user.email },
                            userId = user.userId,
                            userName = user.fullName.ifBlank { user.email },
                            onMessageClick = {
                                activity.startActivity(
                                    MessageDetailsActivity.newIntent(
                                        activity = activity,
                                        otherUserId = user.userId,
                                        otherUserName = user.fullName.ifBlank { user.email },
                                    )
                                )
                            },
                            onVideoCallClick = {
                                startVideoCall(activity, user.userId, user.fullName.ifBlank { user.email })
                            },
                            onVoiceCallClick = {
                                startVoiceCall(activity, user.userId, user.fullName.ifBlank { user.email })
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
    imageRes : Int,
    name : String,
    userId: String,
    userName: String,
    onMessageClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onVoiceCallClick: () -> Unit

){
    Row (
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onMessageClick)
            .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
    ){
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
                .clip(CircleShape)

        )
        Spacer(modifier = Modifier.width(12.dp))
        Column (modifier = Modifier.weight(1f)
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








// Helper functions for starting calls
private fun startVideoCall(activity: Activity, targetUserId: String, targetUserName: String) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
    val currentUserName = auth.currentUser?.email ?: "Me"
    
    // Use current user ID as room ID for direct call
    val callId = currentUserId
    
    val intent = ZegoCallActivity.newIntent(
        activity = activity,
        callId = callId,
        userId = currentUserId,
        userName = currentUserName,
        isVideoCall = true,
        targetUserId = "", // Not needed for direct ZegoCloud
        isIncomingCall = false
    )
    activity.startActivity(intent)
}

private fun startVoiceCall(activity: Activity, targetUserId: String, targetUserName: String) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
    val currentUserName = auth.currentUser?.email ?: "Me"
    
    // Use current user ID as room ID for direct call
    val callId = currentUserId
    
    val intent = ZegoCallActivity.newIntent(
        activity = activity,
        callId = callId,
        userId = currentUserId,
        userName = currentUserName,
        isVideoCall = false,
        targetUserId = "", // Not needed for direct ZegoCloud
        isIncomingCall = false
    )
    activity.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun Preview4(){
    MessageScreen()

}

