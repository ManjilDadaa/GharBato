package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.ReportedRed
import android.widget.Toast
import androidx.compose.foundation.clickable
import com.example.gharbato.repository.ReportUserRepoImpl
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.ReportedUser
import com.example.gharbato.viewmodel.ReportedUsersViewModel

class ReportedUsersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportedUsersScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedUsersScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel = remember { ReportedUsersViewModel(ReportUserRepoImpl(), UserRepoImpl()) }
    
    val users by viewModel.reportedUsers.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(true)
    var userToSuspend by remember { mutableStateOf<ReportedUser?>(null) }
    var userToActivate by remember { mutableStateOf<ReportedUser?>(null) }
    var userToResolve by remember { mutableStateOf<ReportedUser?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadReportedUsers()
    }

    if (userToSuspend != null) {
        SuspendUserDialog(
            user = userToSuspend!!,
            onDismiss = { userToSuspend = null },
            onSuspend = { duration, reason ->
                viewModel.suspendUser(userToSuspend!!.userId, duration, reason) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) userToSuspend = null
                }
            }
        )
    }

    if (userToActivate != null) {
        AlertDialog(
            onDismissRequest = { userToActivate = null },
            title = { Text("Activate User") },
            text = { Text("Are you sure you want to activate ${userToActivate!!.userName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateUser(userToActivate!!.userId) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) userToActivate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToActivate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (userToResolve != null) {
        AlertDialog(
            onDismissRequest = { userToResolve = null },
            title = { Text("Resolve Reports") },
            text = { Text("Are you sure you want to resolve all reports for ${userToResolve!!.userName}? This will remove the user from this list.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveUser(userToResolve!!.userId) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) userToResolve = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Resolve")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToResolve = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Users", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, AdminActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReportedRed)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Reported Users",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${users.size} users flagged",
                                fontSize = 13.sp,
                                color = Gray
                            )
                        }
                    }
                }
            }

            items(users) { user ->

                var showDetails by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color.Red.copy(alpha = 0.1f)
                            ) {
                                if (user.userImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(user.userImage)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Reported User Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.userName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = user.userEmail,
                                    fontSize = 13.sp,
                                    color = Gray
                                )
                            }

                            Surface(
                                color = if (user.accountStatus == "Suspended") Color.Gray.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (user.accountStatus == "Suspended") "Suspended" else "${user.reportCount} Reports",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    color = if (user.accountStatus == "Suspended") Color.Gray else Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(visible = showDetails) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                Text(
                                    text = "Report Reason:",
                                    fontSize = 12.sp,
                                    color = Gray
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = user.reportReason,
                                    fontSize = 14.sp
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Status: ${user.accountStatus}",
                                    fontSize = 12.sp,
                                    color = Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDetails = !showDetails },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(if (showDetails) "Hide" else "View", fontSize = 11.sp, maxLines = 1)
                            }
                            
                            Button(
                                onClick = { userToResolve = user },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text("Resolve", fontSize = 11.sp, maxLines = 1)
                            }

                            Button(
                                onClick = { 
                                    if (user.accountStatus == "Suspended") {
                                        userToActivate = user
                                    } else {
                                        userToSuspend = user 
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.accountStatus == "Suspended") Color(0xFF4CAF50) else Color.Red
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(if (user.accountStatus == "Suspended") "Activate" else "Suspend", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuspendUserDialog(
    user: ReportedUser,
    onDismiss: () -> Unit,
    onSuspend: (Long, String) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(1) } // Default 1 day
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suspend ${user.userName}") },
        text = {
            Column {
                Text("Select suspension duration:")
                Spacer(modifier = Modifier.height(8.dp))
                
                val options = listOf(
                    "24 Hours" to 1,
                    "3 Days" to 3,
                    "1 Week" to 7,
                    "1 Month" to 30,
                    "Permanent" to 36500 // ~100 years
                )
                
                options.forEach { (label, days) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDuration = days }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = (selectedDuration == days),
                            onClick = { selectedDuration = days }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationInMillis = selectedDuration * 24 * 60 * 60 * 1000L
                    onSuspend(durationInMillis, "Violation of community guidelines")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Suspend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
