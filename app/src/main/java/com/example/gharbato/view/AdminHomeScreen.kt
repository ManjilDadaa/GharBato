package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.view.ui.theme.LightBlue
import com.example.gharbato.view.ui.theme.LightGreen
import com.example.gharbato.view.ui.theme.ReportedRed
import com.example.gharbato.viewmodel.ReportViewModel
import com.example.gharbato.viewmodel.ReportViewModelFactory
import com.example.gharbato.repository.PendingPropertiesRepoImpl
import com.example.gharbato.viewmodel.PendingPropertiesViewModel
import com.example.gharbato.viewmodel.PendingPropertiesViewModelFactory
import com.example.gharbato.repository.ReportUserRepoImpl
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.ReportedUsersViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen() {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // ViewModels for real-time data - optimized initialization
    val reportViewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory()
    )

    val pendingViewModel: PendingPropertiesViewModel = viewModel(
        factory = PendingPropertiesViewModelFactory()
    )

    val reportedUsersViewModel: ReportedUsersViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ReportedUsersViewModel(ReportUserRepoImpl(), UserRepoImpl()) as T
            }
        }
    )

    val reportUiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val pendingUiState by pendingViewModel.uiState.collectAsStateWithLifecycle()
    val reportedUsers by reportedUsersViewModel.reportedUsers.observeAsState(emptyList())
    val reportedUsersLoading by reportedUsersViewModel.isLoading.observeAsState(true)

    // Load reported users only once
    LaunchedEffect(Unit) {
        reportedUsersViewModel.loadReportedUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Admin Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ExitToApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.Red
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Logout",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                FirebaseAuth.getInstance().signOut()
                                val intent = Intent(context, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Pending Listings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, PendingListingsActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = LightGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Pending Listings",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    if (pendingUiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Text(
                            "${pendingUiState.properties.size}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp
                            )
                        )
                    }

                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Reported Listings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, ReportedPropertiesActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = LightBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Reported Listings",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    if (reportUiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Text(
                            "${reportUiState.reportedProperties.size}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp
                            )
                        )
                    }

                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Reported Users Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, ReportedUsersActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = ReportedRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Reported Users",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    if (reportedUsersLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Text(
                            "${reportedUsers.size}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp
                            )
                        )
                    }

                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}