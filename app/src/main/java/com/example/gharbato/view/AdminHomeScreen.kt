package com.example.gharbato.view

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ripple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.R
import com.example.gharbato.repository.ReportUserRepoImpl
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.PendingPropertiesViewModel
import com.example.gharbato.viewmodel.PendingPropertiesViewModelFactory
import com.example.gharbato.viewmodel.ReportViewModel
import com.example.gharbato.viewmodel.ReportViewModelFactory
import com.example.gharbato.viewmodel.ReportedUsersViewModel
import com.google.firebase.auth.FirebaseAuth

// Modern color palette
private val PrimaryBlue = Color(0xFF1E88E5)
private val DarkBlue = Color(0xFF1565C0)
private val AccentGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val WarnOrange = Color(0xFFFF9800)
private val DarkOrange = Color(0xFFF57C00)
private val AlertRed = Color(0xFFE53935)
private val DarkRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F7FA)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)

@Composable
fun AdminHomeScreen() {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // ViewModels for real-time data
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

    LaunchedEffect(Unit) {
        reportedUsersViewModel.loadReportedUsers()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Custom Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBlue, DarkBlue)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_admin),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Admin Dashboard",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage your platform",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Outlined.ExitToApp,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
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
                                            modifier = Modifier.size(20.dp),
                                            tint = AlertRed
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Logout",
                                            color = AlertRed,
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
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            PrimaryBlue.copy(alpha = 0.1f),
                                            DarkBlue.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Welcome Back!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Here's what needs your attention today",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Quick Stats Row
                Text(
                    text = "Quick Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total pending items stat
                    val totalPending = pendingUiState.properties.size
                    val totalReported = reportUiState.reportedProperties.size + reportedUsers.size

                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending",
                        count = if (pendingUiState.isLoading) null else totalPending,
                        gradientColors = listOf(AccentGreen, DarkGreen),
                        icon = Icons.Default.Home
                    )

                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Reports",
                        count = if (reportUiState.isLoading || reportedUsersLoading) null else totalReported,
                        gradientColors = listOf(WarnOrange, DarkOrange),
                        icon = Icons.Outlined.Warning
                    )
                }

                // Main Action Cards
                Text(
                    text = "Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                // Pending Listings Card
                DashboardActionCard(
                    title = "Pending Listings",
                    subtitle = "New properties awaiting approval",
                    count = if (pendingUiState.isLoading) null else pendingUiState.properties.size,
                    gradientColors = listOf(AccentGreen, DarkGreen),
                    iconRes = R.drawable.ic_pending,
                    onClick = {
                        val intent = Intent(context, PendingListingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                // Reported Listings Card
                DashboardActionCard(
                    title = "Reported Listings",
                    subtitle = "Properties flagged by users",
                    count = if (reportUiState.isLoading) null else reportUiState.reportedProperties.size,
                    gradientColors = listOf(PrimaryBlue, DarkBlue),
                    iconRes = R.drawable.ic_report,
                    onClick = {
                        val intent = Intent(context, ReportedPropertiesActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                // Reported Users Card
                DashboardActionCard(
                    title = "Reported Users",
                    subtitle = "User accounts under review",
                    count = if (reportedUsersLoading) null else reportedUsers.size,
                    gradientColors = listOf(AlertRed, DarkRed),
                    iconRes = R.drawable.ic_person,
                    onClick = {
                        val intent = Intent(context, ReportedUsersActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int?,
    gradientColors: List<Color>,
    icon: ImageVector
) {
    Card(
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = gradientColors)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    if (count != null) {
                        Text(
                            text = count.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    count: Int?,
    gradientColors: List<Color>,
    iconRes: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = gradientColors.first().copy(alpha = 0.3f),
                spotColor = gradientColors.first().copy(alpha = 0.3f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White)
            ) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (count != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
