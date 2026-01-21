package com.example.gharbato.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.model.NotificationModel
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.utils.NotificationHelper
import com.example.gharbato.viewmodel.UserViewModel
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : ComponentActivity() {

    private lateinit var userViewModel: UserViewModel

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, get FCM token
            getFCMToken()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        userViewModel = UserViewModel(UserRepoImpl())

        // Initialize notification context in ViewModel
        userViewModel.initializeNotificationContext(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        setContent {
            NotificationScreen(userViewModel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    getFCMToken()
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, permission is automatically granted
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Save token to your user's profile in Firebase
                saveFCMToken(token)
            }
        }
    }

    private fun saveFCMToken(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(userId)
                .child("fcmToken")
                .setValue(token)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(userViewModel: UserViewModel = remember { UserViewModel(UserRepoImpl()) }) {
    val context = LocalContext.current

    // Observe LiveData
    val notifications by userViewModel.notifications.observeAsState(emptyList())
    val unreadCount by userViewModel.unreadCount.observeAsState(0)

    var showMenu by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Start real-time observers
    LaunchedEffect(Unit) {
        userViewModel.startObservingNotifications()
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Clear All Notifications",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete all ${notifications.size} notification${if (notifications.size != 1) "s" else ""}? This action cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false

                        // Delete all notifications
                        val notificationIds = notifications.map { it.notificationId }
                        var deleted = 0

                        notificationIds.forEach { notifId ->
                            userViewModel.deleteNotification(notifId)
                            deleted++
                        }

                        Toast.makeText(
                            context,
                            "Cleared $deleted notification${if (deleted != 1) "s" else ""}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        // Show notification count
                        if (notifications.isNotEmpty()) {
                            Text(
                                "${notifications.size} notification${if (notifications.size != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = if (unreadCount > 0) Color(0xFFFF9800) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Blue
                        )
                    }
                },
                actions = {
                    // Show Clear All button only if there are notifications
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_more_horiz_24),
                                contentDescription = "More Options",
                                tint = Blue
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear all") },
                                onClick = {
                                    showMenu = false
                                    showClearAllDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color(0xFFFF3B30)
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_notifications_24),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We'll notify you when something new arrives",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            // Notification List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FB))
            ) {
                items(
                    items = notifications,
                    key = { it.notificationId }
                ) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                userViewModel.markNotificationAsRead(notification.notificationId)
                            }
                        },
                        onDelete = {
                            userViewModel.deleteNotification(notification.notificationId)
                            Toast.makeText(
                                context,
                                "Notification deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Notification",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Are you sure you want to delete this notification?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Background color based on read status
    val backgroundColor = if (notification.isRead) Color.White else Color(0xFFE3F2FD)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon/Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getNotificationColor(notification.type)),
            contentAlignment = Alignment.Center
        ) {
            if (notification.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(notification.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(getNotificationIcon(notification.type)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.title,
                fontSize = 15.sp,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                color = Color(0xFF2C2C2C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                notification.message,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                getTimeAgo(notification.timestamp),
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }

        // Delete Button
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        // Unread Indicator (Blue Dot)
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Blue)
            )
        }
    }
}

fun getNotificationIcon(type: String): Int {
    return when (type) {
        "property" -> R.drawable.baseline_home_24
        "message" -> R.drawable.round_message_24
        "system" -> R.drawable.baseline_settings_24
        "update" -> R.drawable.baseline_info_24
        "listing_approved" -> R.drawable.baseline_home_24
        else -> R.drawable.outline_notifications_24
    }
}

fun getNotificationColor(type: String): Color {
    return when (type) {
        "property" -> Color(0xFF4CAF50)
        "message" -> Color(0xFF2196F3)
        "system" -> Color(0xFFFF9800)
        "update" -> Color(0xFF9C27B0)
        "listing_approved" -> Color(0xFF4CAF50)
        else -> Color(0xFF607D8B)
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}