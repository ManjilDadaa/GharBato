package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gharbato.R
import com.example.gharbato.model.ChatMessage
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MessageDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: "Chat"

        setContent {
            MessageDetailsScreen(
                otherUserId = otherUserId,
                otherUserName = otherUserName,
            )
        }
    }

    companion object {
        const val EXTRA_OTHER_USER_ID = "extra_other_user_id"
        const val EXTRA_OTHER_USER_NAME = "extra_other_user_name"

        fun newIntent(
            activity: Activity,
            otherUserId: String,
            otherUserName: String,
        ): Intent {
            return Intent(activity, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(EXTRA_OTHER_USER_NAME, otherUserName)
            }
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

private fun sanitizeZegoId(value: String): String {
    if (value.isBlank()) return "user"
    return value.replace(Regex("[^A-Za-z0-9_]"), "_")
}

private fun buildChatId(userA: String, userB: String): String {
    val a = sanitizeZegoId(userA)
    val b = sanitizeZegoId(userB)
    return if (a <= b) "${a}_$b" else "${b}_$a"
}

private fun sendImageMessage(
    imageUri: Uri,
    db: FirebaseDatabase,
    chatId: String,
    myUserId: String,
    auth: FirebaseAuth,
    context: Context
) {
    // For now, we'll use the URI directly as imageUrl
    // In a real app, you would upload to Firebase Storage first
    val ref = db.getReference("chats")
        .child(chatId)
        .child("messages")
        .push()

    val message = ChatMessage(
        id = ref.key ?: "",
        senderId = myUserId,
        senderName = auth.currentUser?.email ?: myUserId,
        text = "",
        imageUrl = imageUri.toString(),
        timestamp = System.currentTimeMillis(),
    )

    ref.setValue(message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDetailsScreen(
    otherUserId: String,
    otherUserName: String,
) {
    val context = LocalContext.current
    val activity = context as Activity

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseDatabase.getInstance() }

    val myUserIdRaw = auth.currentUser?.uid ?: getOrCreateLocalUserId(context)
    val myUserId = remember(myUserIdRaw) { sanitizeZegoId(myUserIdRaw) }
    val otherId = remember(otherUserId) { sanitizeZegoId(otherUserId.ifBlank { "other" }) }

    val chatId = remember(myUserId, otherId) { buildChatId(myUserId, otherId) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }
    var showImageOptions by remember { mutableStateOf(false) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sendImageMessage(it, db, chatId, myUserId, auth, context) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Handle camera photo
        }
    }

    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        val query = db.getReference("chats")
            .child(chatId)
            .child("messages")
            .orderByChild("timestamp")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = mutableListOf<ChatMessage>()
                snapshot.children.forEach { child ->
                    val msg = child.getValue(ChatMessage::class.java) ?: return@forEach
                    val id = child.key ?: msg.id
                    newMessages.add(msg.copy(id = id))
                }
                messages.clear()
                messages.addAll(newMessages)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { 
                    Text(
                        text = otherUserName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            activity.startActivity(
                                ZegoCallActivity.newIntent(
                                    activity = activity,
                                    callId = chatId,
                                    userId = myUserId,
                                    userName = auth.currentUser?.email ?: myUserId,
                                    isVideoCall = false,
                                    targetUserId = otherId
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call, 
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            activity.startActivity(
                                ZegoCallActivity.newIntent(
                                    activity = activity,
                                    callId = chatId,
                                    userId = myUserId,
                                    userName = auth.currentUser?.email ?: myUserId,
                                    isVideoCall = true,
                                    targetUserId = otherId
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam, 
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue
                ),
                modifier = Modifier.shadow(4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == myUserId
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.Start else Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = if (isMe) 16.dp else 12.dp,
                                        vertical = 8.dp
                                    ),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                if (!isMe) {
                                    Text(
                                        text = msg.senderName,
                                        color = Color(0xFF6B7280),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(
                                                topStart = if (isMe) 20.dp else 4.dp,
                                                topEnd = if (isMe) 4.dp else 20.dp,
                                                bottomStart = if (isMe) 20.dp else 20.dp,
                                                bottomEnd = if (isMe) 4.dp else 4.dp
                                            )
                                        ),
                                    shape = RoundedCornerShape(
                                        topStart = if (isMe) 20.dp else 4.dp,
                                        topEnd = if (isMe) 4.dp else 20.dp,
                                        bottomStart = if (isMe) 20.dp else 20.dp,
                                        bottomEnd = if (isMe) 4.dp else 4.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) 
                                            Color(0xFF007AFF) 
                                        else 
                                            Color.White
                                    )
                                ) {
                                    // Display image if present
                                    if (msg.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(msg.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Image message",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    
                                    // Display text if present
                                    if (msg.text.isNotEmpty()) {
                                        Text(
                                            text = msg.text,
                                            color = if (isMe) Color.White else Color(0xFF1A1A1A),
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(16.dp),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = formatTimestamp(msg.timestamp),
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(
                                        horizontal = if (isMe) 16.dp else 0.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(25.dp)
                        ),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_person_24),
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { 
                                Text(
                                    "Type a message...",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 16.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Blue
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Image options button
                        Box {
                            IconButton(
                                onClick = { showImageOptions = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Add Image",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showImageOptions,
                                onDismissRequest = { showImageOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gallery") },
                                    onClick = {
                                        showImageOptions = false
                                        galleryLauncher.launch("image/*")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Camera") },
                                    onClick = {
                                        showImageOptions = false
                                        // For camera, we need to create a file URI first
                                        // For now, just launch gallery
                                        galleryLauncher.launch("image/*")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Blue,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val text = messageText.trim()
                                    if (text.isNotEmpty()) {
                                        val ref = db.getReference("chats")
                                            .child(chatId)
                                            .child("messages")
                                            .push()

                                        val message = ChatMessage(
                                            id = ref.key ?: "",
                                            senderId = myUserId,
                                            senderName = auth.currentUser?.email ?: myUserId,
                                            text = text,
                                            timestamp = System.currentTimeMillis(),
                                        )

                                        ref.setValue(message)
                                        messageText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> {
            val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
            date
        }
    }
}
