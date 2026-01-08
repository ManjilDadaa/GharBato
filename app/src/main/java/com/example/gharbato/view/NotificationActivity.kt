package com.example.gharbato.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.DoneAll
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
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.model.NotificationModel
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotificationScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val context = LocalContext.current
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    val notifications by userViewModel.notifications.observeAsState(emptyList())
    val unreadCount by userViewModel.unreadCount.observeAsState(0)

    var showMenu by remember { mutableStateOf(false) }
    var showMarkAllDialog by remember { mutableStateOf(false) }

    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        userViewModel.loadNotifications()
        userViewModel.loadUnreadCount()
    }

    // Mark All as Read Confirmation Dialog
    if (showMarkAllDialog) {
        AlertDialog(
            onDismissRequest = { showMarkAllDialog = false },
            title = { Text("Mark All as Read") },
            text = {
                Text("Are you sure you want to mark all $unreadCount notification${if (unreadCount > 1) "s" else ""} as read?")
            },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.markAllAsRead()
                    showMarkAllDialog = false
                    Toast.makeText(
                        context,
                        "$unreadCount notification${if (unreadCount > 1) "s" else ""} marked as read",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("Mark All", color = Blue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount unread",
                                fontSize = 12.sp,
                                color = Color.Gray
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
                    if (notifications.isNotEmpty() && unreadCount > 0) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_more_24),
                                contentDescription = "More Options",
                                tint = Blue
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark all as read") },
                                onClick = {
                                    showMenu = false
                                    showMarkAllDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DoneAll, contentDescription = null)
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
                    Text(
                        "We'll notify you when something new arrives",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FB))
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                userViewModel.markNotificationAsRead(notification.notificationId)
                            }
                        },
                        onDelete = {
                            userViewModel.deleteNotification(notification.notificationId)
                            Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Extra space at bottom
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
            title = { Text("Delete Notification") },
            text = { Text("Are you sure you want to delete this notification?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (notification.isRead) Color.White else Color(0xFFE3F2FD))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon/Image based on notification type
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

        // Notification content
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

        // Delete button
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

        // Unread indicator
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Blue)
            )
        }
    }
}

// Helper function to get notification icon based on type
fun getNotificationIcon(type: String): Int {
    return when (type) {
        "property" -> R.drawable.baseline_home_24
        "message" -> R.drawable.round_message_24
        "system" -> R.drawable.baseline_settings_24
        "update" -> R.drawable.baseline_info_24
        else -> R.drawable.outline_notifications_24
    }
}

// Helper function to get notification color based on type
fun getNotificationColor(type: String): Color {
    return when (type) {
        "property" -> Color(0xFF4CAF50)
        "message" -> Color(0xFF2196F3)
        "system" -> Color(0xFFFF9800)
        "update" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }
}

// Helper function to format timestamp
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