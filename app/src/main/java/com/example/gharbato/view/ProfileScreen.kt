package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Black
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize ViewModel
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    // Observe user data and unread notifications
    val userData by userViewModel.userData.observeAsState()
    val unreadCount by userViewModel.unreadCount.observeAsState(0) // 🔥 Live updates

    var showContactInfo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Load profile & unread count on screen launch
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        userViewModel.loadUnreadCount()
        isLoading = false
    }

    // Reload profile & unread count on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userViewModel.loadUserProfile()
                userViewModel.loadUnreadCount() // 🔥 refresh badge
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {
        // Top Bar with Notification Badge
        TopAppBar(
            title = {
                Text(
                    "Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            ),
            actions = {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(context, NotificationActivity::class.java))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_notifications_24),
                            contentDescription = "Notifications",
                            tint = Blue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Badge
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 6.dp)
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .border(2.dp, Color.White, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        )

        if (isLoading || userData == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // -------- Profile Header --------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = if (userData?.profileImageUrl?.isNotEmpty() == true) {
                                rememberAsyncImagePainter(userData?.profileImageUrl)
                            } else {
                                painterResource(R.drawable.billu)
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Blue),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Blue)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    context.startActivity(
                                        Intent(context, EditProfileActivity::class.java)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            userData?.fullName ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            if (showContactInfo) "Hide contact info" else "Show contact info",
                            fontSize = 13.sp,
                            color = Color(0xFF4D8DFF),
                            modifier = Modifier.clickable {
                                showContactInfo = !showContactInfo
                            }
                        )
                    }
                }

                if (showContactInfo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        ContactInfoRow(
                            icon = R.drawable.baseline_email_24,
                            label = "Email",
                            value = userData?.email ?: "N/A"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ContactInfoRow(
                            icon = R.drawable.baseline_email_24,
                            label = "Phone",
                            value = userData?.phoneNo ?: "N/A"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ContactInfoRow(
                            icon = R.drawable.baseline_email_24,
                            label = "Country",
                            value = userData?.selectedCountry ?: "N/A"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // -------- Profile Settings --------
                SectionHeader("Profile Settings")

                CleanMenuItem(
                    icon = R.drawable.baseline_watch_24,
                    title = "My Activities",
                    subtitle = "View your account activities",
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, MyActivitiesActivity::class.java))
                }

                CleanMenuItem(
                    icon = R.drawable.baseline_create_24,
                    title = "Trust and Verification",
                    subtitle = "Manage your account security",
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, TrustAndVerificationActivity::class.java))
                }

                CleanMenuItem(
                    icon = R.drawable.baseline_settings_24,
                    title = "Application Settings",
                    subtitle = "Configure your app settings",
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader("Support")

                CleanMenuItem(
                    icon = R.drawable.outline_adb_24,
                    title = "Help Center",
                    subtitle = null,
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, HelpCenterActivity::class.java))
                }

                CleanMenuItem(
                    icon = R.drawable.baseline_email_24,
                    title = "Terms & Policies",
                    subtitle = null,
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, TermsAndPoliciesActivity::class.java))
                }

                CleanMenuItem(
                    icon = R.drawable.baseline_email_24,
                    title = "Contact Us",
                    subtitle = null,
                    iconColor = Blue
                ) {
                    context.startActivity(Intent(context, ContactUsActivity::class.java))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            FirebaseAuth.getInstance().signOut()
                            val loginIntent = Intent(context, LoginActivity::class.java)
                            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(loginIntent)
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_logout_24),
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Logout",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE53935)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ----- Reused Components -----
@Composable
fun ContactInfoRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color(0xFF4D8DFF),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color(0xFF999999))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C2C2C)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2C2C2C),
        modifier = modifier.padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
    )
}

@Composable
fun CleanMenuItem(
    icon: Int,
    title: String,
    subtitle: String?,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2C2C2C))
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = Color(0xFF999999))
                }
            }
            Icon(
                painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
