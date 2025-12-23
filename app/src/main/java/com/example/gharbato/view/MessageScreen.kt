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
    
    // Load users using repository pattern
    LaunchedEffect(Unit) {
        userViewModel.getAllUsers { success, userList, message ->
            if (success && userList != null) {
                android.util.Log.d("MessageScreen", "Fetched ${userList.size} users: ${userList.map { it.fullName }}")
                users = userList
                errorMessage = ""
            } else {
                android.util.Log.e("MessageScreen", "Error fetching users: $message")
                errorMessage = message
            }
            isLoading = false
        }
    }
    
    // Search users when query changes
    LaunchedEffect(searchText) {
        if (isLoading) return@LaunchedEffect
        android.util.Log.d("MessageScreen", "Searching users with query: '$searchText'")
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
            } else if (errorMessage.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $errorMessage", color = Color.Red)
                }
            } else {
                LazyColumn {
                    // "Me" entry for self-chat
                    item {
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = "Me"
                        ) {
                            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                            val myId = auth.currentUser?.uid ?: getOrCreateLocalUserId(activity)
                            val myName = auth.currentUser?.email ?: "Me"

                            activity.startActivity(
                                MessageDetailsActivity.newIntent(
                                    activity = activity,
                                    otherUserId = myId,
                                    otherUserName = myName,
                                )
                            )
                        }
                    }
                    
                    // Firebase users
                    items(users) { user ->
                        MessageUserItem(
                            imageRes = R.drawable.outline_person_24,
                            name = user.fullName.ifBlank { user.email }
                        ) {
                            activity.startActivity(
                                MessageDetailsActivity.newIntent(
                                    activity = activity,
                                    otherUserId = user.userId,
                                    otherUserName = user.fullName.ifBlank { user.email },
                                )
                            )
                        }
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
    onclick: () -> Unit

){
    Row (
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onclick)
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
        Column (modifier = Modifier.fillMaxWidth()
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
    }
}








@Preview(showBackground = true)
@Composable
fun Preview4(){
    MessageScreen()

}

