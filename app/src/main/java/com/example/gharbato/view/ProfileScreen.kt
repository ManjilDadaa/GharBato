package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Black
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ProfileScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current

    // Initialize ViewModel
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    // Observe user data from ViewModel
    val userData by userViewModel.userData.observeAsState()

    var showContactInfo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Load user profile when screen opens
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        isLoading = false
    }

    // Reload profile when returning from EditProfileActivity
    DisposableEffect(Unit) {
        val lifecycleOwner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userViewModel.loadUserProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
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
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(R.drawable.outline_notifications_24),
                            contentDescription = "Notifications",
                            tint = Blue
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading || userData == null) {
            // Show loading indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FB))
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image with Edit Icon
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = painterResource(R.drawable.billu),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Blue),
                            contentScale = ContentScale.Crop
                        )

                        // Edit Icon Button
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

                        // Show/Hide contact info
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

                // Contact Info Section (Expandable)
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

                // Profile Settings Section
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

                // Support Section
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

                // Logout Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            // Sign out from Firebase
                            FirebaseAuth.getInstance().signOut()

                            val loginIntent = Intent(context, LoginActivity::class.java)
                            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(loginIntent)
                            (context as ComponentActivity).finish()
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

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

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
        modifier = modifier.padding(
            start = 12.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 12.dp
        )
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
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C2C2C)
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
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