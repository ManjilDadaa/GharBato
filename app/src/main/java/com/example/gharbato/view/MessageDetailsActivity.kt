package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gharbato.model.ChatMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.MessageDetailsViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.FileProvider
import coil.request.ImageRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MessageDetailsActivity"
        private const val EXTRA_OTHER_USER_ID = "other_user_id"
        private const val EXTRA_OTHER_USER_NAME = "other_user_name"
        private const val EXTRA_OTHER_USER_IMAGE = "other_user_image"

        fun newIntent(
            activity: Activity,
            otherUserId: String,
            otherUserName: String,
            otherUserImage: String = ""
        ): Intent {
            return Intent(activity, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(EXTRA_OTHER_USER_NAME, otherUserName)
                putExtra(EXTRA_OTHER_USER_IMAGE, otherUserImage)
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: ""
        val otherUserImage = intent.getStringExtra(EXTRA_OTHER_USER_IMAGE) ?: ""

        val currentUserId = auth.currentUser?.uid ?: ""

        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            Log.e(TAG, "User IDs are empty! Current: $currentUserId, Other: $otherUserId")
            finish()
            return
        }

        setContent {
            MessageDetailsScreen(
                currentUserId = currentUserId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserImage = otherUserImage,
                onBackClick = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailsScreen(
    currentUserId: String,
    otherUserId: String,
    otherUserName: String,
    otherUserImage: String,
    onBackClick: () -> Unit,
    viewModel: MessageDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages
    val messageText by viewModel.messageText
    val isBlockedByMe by viewModel.isBlockedByMe
    val isBlockedByOther by viewModel.isBlockedByOther
    
    val listState = rememberLazyListState()
    
    var showReportDialog by remember { mutableStateOf(false) }

    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            Toast.makeText(context, "Sending photo...", Toast.LENGTH_SHORT).show()
            viewModel.sendImageMessage(context, it) 
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            Toast.makeText(context, "Sending photo...", Toast.LENGTH_SHORT).show()
            viewModel.sendImageMessage(context, currentPhotoUri!!)
        }
    }

    fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        currentPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(otherUserId) {
        viewModel.startChat(context, otherUserId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showReportDialog) {
        ReportUserDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                viewModel.reportUser(reason) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                showReportDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                userName = otherUserName,
                userImage = otherUserImage,
                isBlockedByMe = isBlockedByMe,
                onBackClick = onBackClick,
                onBlockClick = { viewModel.toggleBlockUser() },
                onDeleteClick = { viewModel.deleteChat() },
                onReportClick = { showReportDialog = true },
                onAudioCallClick = { viewModel.initiateCall(context as Activity, false, otherUserName) },
                onVideoCallClick = { viewModel.initiateCall(context as Activity, true, otherUserName) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId
                    )
                }
            }

            // Message Input
            if (isBlockedByMe || isBlockedByOther) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBlockedByMe) "You blocked this user" else "You have been blocked",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                MessageInput(
                    messageText = messageText,
                    onMessageTextChange = { viewModel.onMessageTextChanged(it) },
                    onSendClick = { viewModel.sendTextMessage() },
                    onCameraClick = { launchCamera() },
                    onAttachClick = { imagePickerLauncher.launch("image/*") }
                )
            }
        }
    }
}

@Composable
fun ReportUserDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report User") },
        text = {
            Column {
                Text("Why are you reporting this user?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(reason) },
                enabled = reason.isNotBlank()
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    userName: String,
    userImage: String,
    isBlockedByMe: Boolean,
    onBackClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (userImage.isNotEmpty()) {
                        AsyncImage(
                            model = userImage,
                            contentDescription = userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Column {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        },
        actions = {
            IconButton(onClick = onVideoCallClick) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Video Call",
                    tint = Color.Black
                )
            }
            IconButton(onClick = onAudioCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = Color.Black
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, "Menu", tint = Color.Black)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Report User") },
                    onClick = { 
                        menuExpanded = false
                        onReportClick() 
                    },
                    leadingIcon = { Icon(Icons.Default.Report, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete Chat") },
                    onClick = { 
                        menuExpanded = false
                        onDeleteClick() 
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (isBlockedByMe) "Unblock User" else "Block User") },
                    onClick = { 
                        menuExpanded = false
                        onBlockClick() 
                    },
                    leadingIcon = { Icon(Icons.Default.Block, null) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}



@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Blue else Color.White,
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Shared Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_report_image) // Fallback
                    )
                    if (message.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(message.timestamp),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach File",
                    tint = Color.Gray
                )
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Blue,
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5)
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Blue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Blue else Color.White,
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(message.timestamp),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Blue,
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5)
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Blue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

private fun sendMessage(
    chatId: String,
    currentUserId: String,
    otherUserId: String,
    messageText: String,
    messagesRef: DatabaseReference
) {
    val messageId = messagesRef.push().key ?: return

    val message = hashMapOf(
        "senderId" to currentUserId,
        "receiverId" to otherUserId,
        "message" to messageText,
        "timestamp" to ServerValue.TIMESTAMP,
        "isRead" to false
    )

    Log.d("MessageDetails", "Sending message to chat: $chatId")
    Log.d("MessageDetails", "From: $currentUserId, To: $otherUserId")

    messagesRef
        .child(messageId)
        .setValue(message)
        .addOnSuccessListener {
            Log.d("MessageDetails", "Message sent successfully")
        }
        .addOnFailureListener { e ->
            Log.e("MessageDetails", "Failed to send message", e)
        }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val now = Calendar.getInstance()

    return when {
        isSameDay(calendar, now) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        }
        isYesterday(calendar, now) -> {
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(calendar.time)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val yesterday = cal2.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, yesterday)
}
