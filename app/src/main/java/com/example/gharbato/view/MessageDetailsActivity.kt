package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
        val otherUserImage = intent.getStringExtra(EXTRA_OTHER_USER_IMAGE) ?: ""

        setContent {
            MessageDetailsScreen(
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserImage = otherUserImage,
            )
        }
    }

    companion object {
        const val EXTRA_OTHER_USER_ID = "extra_other_user_id"
        const val EXTRA_OTHER_USER_NAME = "extra_other_user_name"
        const val EXTRA_OTHER_USER_IMAGE = "extra_other_user_image"

        fun newIntent(
            activity: Activity,
            otherUserId: String,
            otherUserName: String,
            otherUserImage: String = "",
        ): Intent {
            return Intent(activity, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(EXTRA_OTHER_USER_NAME, otherUserName)
                putExtra(EXTRA_OTHER_USER_IMAGE, otherUserImage)
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

private fun createImageFileUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "chat_images")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDetailsScreen(
    otherUserId: String,
    otherUserName: String,
    otherUserImage: String,
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

    // Camera launcher with file URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            sendImageMessage(cameraImageUri!!, db, chatId, myUserId, auth, context)
            cameraImageUri = null
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFEFE7DE) // WhatsApp default background color
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Custom Top Bar matching WhatsApp design
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activity.finish() },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* Handle profile click */ }
                            .padding(start = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (otherUserImage.isNotEmpty()) {
                                AsyncImage(
                                    model = otherUserImage,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = otherUserName,
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "last seen today at 6:22 PM",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Actions
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
                            contentDescription = "Video Call",
                            tint = Color.Black
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
                                    isVideoCall = false,
                                    targetUserId = otherId
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = Color.Black
                        )
                    }

                    // Removed More options icon as requested
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp), // Reduced padding for WhatsApp style
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp), // Closer spacing like WhatsApp
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == myUserId
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom // Align profile pic with bottom of bubble
                        ) {
                            if (!isMe) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(28.dp) // Smaller than header
                                        .clip(CircleShape)
                                        .background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (otherUserImage.isNotEmpty()) {
                                        AsyncImage(
                                            model = otherUserImage,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 300.dp) // Limit max width
                                    .shadow(
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 12.dp
                                        )
                                    ),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) 
                                        Color(0xFFDCF8C6) // WhatsApp outgoing green
                                    else 
                                        Color.White // WhatsApp incoming white
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    // Image Content
                                    if (msg.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(msg.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Image message",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .padding(bottom = 4.dp),
                                            contentScale =  ContentScale.Crop
                                        )
                                    }
                                    
                                    // Text Content and Metadata Row
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.wrapContentSize()
                                    ) {
                                        if (msg.text.isNotEmpty()) {
                                            Text(
                                                text = msg.text,
                                                color = Color.Black,
                                                fontSize = 16.sp,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .weight(1f, fill = false), // Allow text to take space but not force row expansion
                                                lineHeight = 22.sp
                                            )
                                        }

                                        // Timestamp and Status
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = formatTimestamp(msg.timestamp),
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Input Card
                Card(
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message", color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp, max = 120.dp), // Auto-grow
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color.Black
                            ),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions.Default
                        )

                        IconButton(onClick = { showImageOptions = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_attach_file),
                                contentDescription = "Attach",
                                tint = Color.Gray,
                                modifier = Modifier.rotate(45f) // Paperclip rotated
                            )
                        }
                        
                        // Dropdown for attachment options
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
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Camera") },
                                onClick = { 
                                    showImageOptions = false
                                    val uri = createImageFileUri(context)
                                    cameraImageUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                }
                            )
                        }

                        if (messageText.isEmpty()) {
                            IconButton(onClick = { 
                                val uri = createImageFileUri(context)
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Send Button (always show Send icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A884))
                        .clickable {
                            if (messageText.isNotBlank()) {
                                val ref = db.getReference("chats")
                                    .child(chatId)
                                    .child("messages")
                                    .push()

                                val message = ChatMessage(
                                    id = ref.key ?: "",
                                    senderId = myUserId,
                                    senderName = auth.currentUser?.email ?: myUserId,
                                    text = messageText,
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
                        contentDescription = "Send",
                        tint = Color.White
                    )
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
