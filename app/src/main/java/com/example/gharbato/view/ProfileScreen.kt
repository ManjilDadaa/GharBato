package com.example.gharbato.view

import com.example.gharbato.ui.theme.LocalGharBatoActionColors
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
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.viewmodel.UserViewModelProvider
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    GharBatoTheme(darkTheme = isDarkMode) {
        ProfileScreenContent(isDarkMode)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(isDarkMode: Boolean) {
    val context = LocalContext.current
    val userViewModel = remember { UserViewModelProvider.getInstance() }
    val userData by userViewModel.userData.observeAsState()
    val unreadCount by userViewModel.unreadCount.observeAsState(0)
    var showContactInfo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Colors based on theme
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackgroundColor = MaterialTheme.colorScheme.surface
    val textColorPrimary = MaterialTheme.colorScheme.onBackground
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val brandColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        userViewModel.startObservingNotifications()
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose { userViewModel.stopObservingNotifications() }
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        TopAppBar(
            title = {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColorPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor
            ),
            actions = {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(
                        onClick = { context.startActivity(Intent(context, ListingActivity::class.java)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = "Add Listing",
                            tint = brandColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )

        if (isLoading || userData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = brandColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header Section
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
                                painterResource(R.drawable.profile)
                            },
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(brandColor),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    context.startActivity(Intent(context, EditProfileActivity::class.java))
                                },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = LocalGharBatoActionColors.current.onContainer,
                                modifier = Modifier.size(28.dp)
                                    .clip(CircleShape)
                                    .background(LocalGharBatoActionColors.current.container)
                                    .border(2.dp, cardBackgroundColor, CircleShape)
                                    .padding(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            userData?.fullName ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColorPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (showContactInfo) "Hide contact info" else "Show contact info",
                            fontSize = 13.sp,
                            color = brandColor,
                            modifier = Modifier.clickable { showContactInfo = !showContactInfo }
                        )
                    }
                }

                // Contact Info Card
                if (showContactInfo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBackgroundColor)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        ContactInfoRow(
                            R.drawable.baseline_email_24,
                            "Email",
                            userData?.email ?: "N/A",
                            isDarkMode
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ContactInfoRow(
                            R.drawable.baseline_phone_24,
                            "Phone",
                            userData?.phoneNo ?: "N/A",
                            isDarkMode
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ContactInfoRow(
                            R.drawable.baseline_location_on_24,
                            "Country",
                            userData?.selectedCountry ?: "N/A",
                            isDarkMode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Profile Settings Section
                ProfileSectionHeader("Profile Settings", isDarkMode)
                CleanMenuItem(
                    R.drawable.baseline_watch_24,
                    "My Activities",
                    "View your account activities",
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, MyActivitiesActivity::class.java))
                }
                CleanMenuItem(
                    R.drawable.baseline_verified_user_24,
                    "Trust and Verification",
                    "Manage your account security",
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, TrustAndVerificationActivity::class.java))
                }
                CleanMenuItem(
                    R.drawable.baseline_settings_24,
                    "Application Settings",
                    "Configure your app settings",
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Support Section
                ProfileSectionHeader("Support", isDarkMode)
                CleanMenuItem(
                    R.drawable.outline_adb_24,
                    "Help Center",
                    null,
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, HelpCenterActivity::class.java))
                }
                CleanMenuItem(
                    R.drawable.baseline_description_24,
                    "Terms & Policies",
                    null,
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, TermsAndPoliciesActivity::class.java))
                }
                CleanMenuItem(
                    R.drawable.baseline_email_24,
                    "Contact Us",
                    null,
                    brandColor,
                    isDarkMode,
                    cardBackgroundColor,
                    textColorPrimary,
                    textColorSecondary
                ) {
                    context.startActivity(Intent(context, ContactUsActivity::class.java))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Logout Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackgroundColor)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            userViewModel.stopObservingNotifications()
                            FirebaseAuth.getInstance().signOut()
                            UserViewModelProvider.clearInstance()
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.baseline_logout_24),
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Logout",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ContactInfoRow(icon: Int, label: String, value: String, isDarkMode: Boolean) {
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textColorPrimary = MaterialTheme.colorScheme.onSurface
    val iconColor = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(icon),
            null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                fontSize = 13.sp,
                color = textColorSecondary
            )
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColorPrimary
            )
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String, isDarkMode: Boolean) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier.padding(
            start = 20.dp,
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
    isDarkMode: Boolean,
    cardBackgroundColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val arrowColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(icon),
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColorPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = textColorSecondary
                    )
                }
            }
            Icon(
                painterResource(R.drawable.outline_arrow_forward_ios_24),
                null,
                tint = arrowColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
