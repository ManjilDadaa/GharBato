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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.gharbato.model.ReportUser
import com.example.gharbato.model.UserModel
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.ReportedRed

class ReportedUsersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportedUsersScreen()
        }
    }
}

data class ReportedUser(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userImage: String,
    val reportCount: Int,
    val reportReason: String, // Latest or primary reason
    val accountStatus: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedUsersScreen() {
    var users by remember { mutableStateOf<List<ReportedUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val activity = context as Activity

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val reportsRef = database.getReference("user_reports")
        val usersRef = database.getReference("Users")

        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reportsMap = mutableMapOf<String, MutableList<ReportUser>>()
                
                // Group reports by reportedUserId
                for (child in snapshot.children) {
                    val report = child.getValue(ReportUser::class.java)
                    if (report != null && report.reportedUserId.isNotEmpty()) {
                        if (!reportsMap.containsKey(report.reportedUserId)) {
                            reportsMap[report.reportedUserId] = mutableListOf()
                        }
                        reportsMap[report.reportedUserId]?.add(report)
                    }
                }

                val reportedUsersList = mutableListOf<ReportedUser>()
                var processedCount = 0
                val totalUsersToFetch = reportsMap.size

                if (totalUsersToFetch == 0) {
                    users = emptyList()
                    isLoading = false
                    return
                }

                // Fetch user details for each reported user
                for ((userId, userReports) in reportsMap) {
                    usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val userModel = userSnapshot.getValue(UserModel::class.java)
                            val userName = userModel?.fullName ?: "Unknown User"
                            val userEmail = userModel?.email ?: "No Email"
                            val userImage = userModel?.profileImageUrl ?: ""
                            
                            val latestReport = userReports.maxByOrNull { it.timestamp }
                            val reason = latestReport?.reason ?: "No reason provided"

                            reportedUsersList.add(
                                ReportedUser(
                                    userId = userId,
                                    userName = userName,
                                    userEmail = userEmail,
                                    userImage = userImage,
                                    reportCount = userReports.size,
                                    reportReason = reason,
                                    accountStatus = "Active" // You might want to fetch this from userModel if it exists
                                )
                            )

                            processedCount++
                            if (processedCount == totalUsersToFetch) {
                                users = reportedUsersList
                                isLoading = false
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            processedCount++
                            if (processedCount == totalUsersToFetch) {
                                users = reportedUsersList
                                isLoading = false
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
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
                                color = Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${user.reportCount} Reports",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    color = Color.Red,
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
//                                    text = "Reported on: ${user.reportedDate}",
                                    text = "Reported on: xxxx-xx-xx}",
                                    fontSize = 12.sp,
                                    color = Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDetails = !showDetails },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (showDetails) "Hide Details" else "View Details")
                            }

                            Button(
                                onClick = { /* Suspend user */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Suspend")
                            }
                        }
                    }
                }
            }
        }
    }
}
