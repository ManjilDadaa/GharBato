package com.example.gharbato.view

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

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
    val reportCount: Int,
    val reportReason: String,
    val accountStatus: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedUsersScreen() {
    val users = remember {
        listOf(
            ReportedUser("u1", "Spam User", "spam@example.com", 5, "Posting spam content", "Active"),
            ReportedUser("u2", "Fake Account", "fake@example.com", 3, "Fraudulent activity", "Active")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Users", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red)
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
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Color.Red, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Reported Users", 18.sp, fontWeight = FontWeight.Bold)
                            Text("${users.size} users flagged", 13.sp, Gray)
                        }
                    }
                }
            }

            items(users) { user ->
                var showDetails by remember { mutableStateOf(false) }

                Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), CircleShape, Color.Red.copy(0.1f)) {
                                Box(Alignment.Center) {
                                    Text(
                                        user.userName.first().toString(),
                                        Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(user.userEmail, 13.sp, Gray)
                            }
                            Surface(Color.Red.copy(0.1f), RoundedCornerShape(8.dp)) {
                                Text(
                                    "${user.reportCount} Reports",
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    Color.Red,
                                    12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(showDetails) {
                            Column {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                Text("Report Reason:", 12.sp, Gray)