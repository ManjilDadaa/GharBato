package com.example.gharbato.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.gharbato.model.KycModel
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern color palette for Verify Screen
private val PrimaryGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val AccentGreen = Color(0xFF66BB6A)
private val PendingOrange = Color(0xFFFF9800)
private val DarkOrange = Color(0xFFF57C00)
private val RejectedRed = Color(0xFFE53935)
private val DarkRed = Color(0xFFC62828)
private val ApprovedGreen = Color(0xFF4CAF50)
private val BackgroundGray = Color(0xFFF5F7FA)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)

@Composable
fun VerifyUserScreen() {
    val context = LocalContext.current
    val kycViewModel = remember { KycViewModel(KycRepoImpl()) }
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    val kycSubmissions by kycViewModel.kycSubmissions.observeAsState(emptyList())
    val loading by kycViewModel.loading.observeAsState(false)

    var selectedTab by remember { mutableStateOf("Pending") }
    var selectedKyc by remember { mutableStateOf<KycModel?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    // Filter submissions based on selected tab
    val filteredSubmissions = remember(kycSubmissions, selectedTab) {
        kycSubmissions.filter { it.status == selectedTab }
    }

    LaunchedEffect(Unit) {
        kycViewModel.loadAllKycSubmissions()
    }

    // Image Dialog
    if (showImageDialog) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column {
                    // Dialog Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PrimaryGreen, DarkGreen)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Document Image",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showImageDialog = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    AsyncImage(
                        model = selectedImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Reject Dialog
    if (showRejectDialog && selectedKyc != null) {
        Dialog(
            onDismissRequest = {
                showRejectDialog = false
                rejectionReason = ""
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header with Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RejectedRed.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = RejectedRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Reject KYC",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    selectedKyc!!.userName,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                showRejectDialog = false
                                rejectionReason = ""
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reason Input
                    Text(
                        "Rejection Reason",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = {
                            Text(
                                "Enter a clear reason for rejection...",
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RejectedRed,
                            unfocusedBorderColor = BackgroundGray,
                            cursorColor = RejectedRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showRejectDialog = false
                                rejectionReason = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, BackgroundGray)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }

                        VerifyActionButton(
                            text = "Reject",
                            icon = Icons.Default.Close,
                            gradientColors = listOf(RejectedRed, DarkRed),
                            enabled = rejectionReason.isNotBlank(),
                            onClick = {
                                if (rejectionReason.isNotBlank() && selectedKyc != null) {
                                    val kycToReject = selectedKyc!!
                                    kycViewModel.updateKycStatus(
                                        kycId = kycToReject.kycId,
                                        status = "Rejected",
                                        reviewedBy = "Admin",
                                        rejectionReason = rejectionReason
                                    ) { success, message ->
                                        if (success) {
                                            userViewModel.createNotificationForUser(
                                                userId = kycToReject.userId,
                                                title = "KYC Rejected",
                                                message = "Your KYC verification was rejected. Reason: $rejectionReason",
                                                type = "system"
                                            ) { _, _ -> }
                                            Toast.makeText(context, "KYC rejected", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed: $message", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    showRejectDialog = false
                                    rejectionReason = ""
                                    selectedKyc = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
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
            // Custom Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryGreen, DarkGreen)
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
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "KYC Verifications",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Review user submissions",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = { kycViewModel.loadAllKycSubmissions() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tab Cards Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerifyTabCard(
                        title = "Pending",
                        count = kycSubmissions.count { it.status == "Pending" },
                        icon = Icons.Default.HourglassEmpty,
                        gradientColors = listOf(PendingOrange, DarkOrange),
                        isSelected = selectedTab == "Pending",
                        onClick = { selectedTab = "Pending" },
                        modifier = Modifier.weight(1f)
                    )
                    VerifyTabCard(
                        title = "Approved",
                        count = kycSubmissions.count { it.status == "Approved" },
                        icon = Icons.Default.CheckCircle,
                        gradientColors = listOf(ApprovedGreen, DarkGreen),
                        isSelected = selectedTab == "Approved",
                        onClick = { selectedTab = "Approved" },
                        modifier = Modifier.weight(1f)
                    )
                    VerifyTabCard(
                        title = "Rejected",
                        count = kycSubmissions.count { it.status == "Rejected" },
                        icon = Icons.Default.ErrorOutline,
                        gradientColors = listOf(RejectedRed, DarkRed),
                        isSelected = selectedTab == "Rejected",
                        onClick = { selectedTab = "Rejected" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedTab Submissions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (selectedTab) {
                                "Pending" -> PendingOrange
                                "Approved" -> ApprovedGreen
                                else -> RejectedRed
                            }.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        filteredSubmissions.size.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedTab) {
                            "Pending" -> PendingOrange
                            "Approved" -> ApprovedGreen
                            else -> RejectedRed
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Content
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    loading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryGreen,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Loading submissions...",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    filteredSubmissions.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (selectedTab) {
                                            "Pending" -> PendingOrange
                                            "Approved" -> ApprovedGreen
                                            else -> RejectedRed
                                        }.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (selectedTab) {
                                        "Pending" -> Icons.Default.HourglassEmpty
                                        "Approved" -> Icons.Default.CheckCircle
                                        else -> Icons.Default.ErrorOutline
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = when (selectedTab) {
                                        "Pending" -> PendingOrange
                                        "Approved" -> ApprovedGreen
                                        else -> RejectedRed
                                    }.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "No $selectedTab Submissions",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "All submissions will appear here",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredSubmissions) { kyc ->
                                EnhancedKycCard(
                                    kyc = kyc,
                                    onApprove = {
                                        kycViewModel.updateKycStatus(
                                            kycId = kyc.kycId,
                                            status = "Approved",
                                            reviewedBy = "Admin"
                                        ) { success, message ->
                                            if (success) {
                                                userViewModel.createNotificationForUser(
                                                    userId = kyc.userId,
                                                    title = "KYC Approved",
                                                    message = "Congratulations! Your KYC verification has been approved. You can now access all features.",
                                                    type = "system"
                                                ) { _, _ -> }
                                                Toast.makeText(context, "KYC approved", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed: $message", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onReject = {
                                        selectedKyc = kyc
                                        showRejectDialog = true
                                    },
                                    onViewImage = { imageUrl ->
                                        selectedImageUrl = imageUrl
                                        showImageDialog = true
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifyTabCard(
    title: String,
    count: Int,
    icon: ImageVector,
    gradientColors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Transparent else BackgroundGray
        ),
        border = if (!isSelected) BorderStroke(1.dp, BackgroundGray) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.background(
                            brush = Brush.linearGradient(colors = gradientColors)
                        )
                    } else Modifier
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else gradientColors.first()
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                )
            }
        }
    }
}

@Composable
fun EnhancedKycCard(
    kyc: KycModel,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewImage: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (kyc.status) {
        "Approved" -> ApprovedGreen
        "Rejected" -> RejectedRed
        else -> PendingOrange
    }

    val statusGradient = when (kyc.status) {
        "Approved" -> listOf(ApprovedGreen, DarkGreen)
        "Rejected" -> listOf(RejectedRed, DarkRed)
        else -> listOf(PendingOrange, DarkOrange)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = statusColor.copy(alpha = 0.1f),
                spotColor = statusColor.copy(alpha = 0.1f)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            kyc.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            kyc.userEmail,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.horizontalGradient(colors = statusGradient)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        kyc.status,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Document Type",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            kyc.documentType,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Submitted",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                formatDate(kyc.submittedAt),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Images
            Text(
                "Document Images",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedDocumentPreview(
                    title = "Front Side",
                    imageUrl = kyc.frontImageUrl,
                    onClick = { onViewImage(kyc.frontImageUrl) },
                    modifier = Modifier.weight(1f)
                )
                EnhancedDocumentPreview(
                    title = "Back Side",
                    imageUrl = kyc.backImageUrl,
                    onClick = { onViewImage(kyc.backImageUrl) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Rejection Reason (if rejected)
            if (kyc.status == "Rejected" && kyc.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RejectedRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            null,
                            tint = RejectedRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Rejection Reason",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RejectedRed
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                kyc.rejectionReason,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Action Buttons (only for Pending)
            if (kyc.status == "Pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VerifyActionButton(
                        text = "Approve",
                        icon = Icons.Default.Check,
                        gradientColors = listOf(ApprovedGreen, DarkGreen),
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RejectedRed
                        ),
                        border = BorderStroke(1.5.dp, RejectedRed)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Reject",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedDocumentPreview(
    title: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundGray)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // View overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "View",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VerifyActionButton(
    text: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(colors = gradientColors)
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Gray.copy(alpha = 0.5f),
                            Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
