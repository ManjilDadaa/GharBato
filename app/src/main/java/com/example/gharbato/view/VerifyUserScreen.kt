package com.example.gharbato.view

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.gharbato.model.KycModel
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyUserScreen() {
    val context = LocalContext.current
    val kycViewModel = remember { KycViewModel(KycRepoImpl()) }
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    
    val kycSubmissions by kycViewModel.kycSubmissions.observeAsState(emptyList())
    val loading by kycViewModel.loading.observeAsState(false)
    
    var selectedKyc by remember { mutableStateOf<KycModel?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        kycViewModel.loadAllKycSubmissions()
    }
    
    // Image Dialog
    if (showImageDialog) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Document Image", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showImageDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
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
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject KYC") },
            text = {
                Column {
                    Text("Please provide a reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("Enter rejection reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
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
                                        title = "❌ KYC Rejected",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRejectDialog = false
                    rejectionReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FB))
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "KYC Verifications",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                IconButton(
                    onClick = { kycViewModel.loadAllKycSubmissions() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Blue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total",
                    count = kycSubmissions.size,
                    color = Blue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending",
                    count = kycSubmissions.count { it.status == "Pending" },
                    color = Color(0xFFFFA000),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Approved",
                    count = kycSubmissions.count { it.status == "Approved" },
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (kycSubmissions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No KYC submissions found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(kycSubmissions) { kyc ->
                        KycCard(
                            kyc = kyc,
                            onApprove = {
                                kycViewModel.updateKycStatus(
                                    kycId = kyc.kycId,
                                    status = "Approved",
                                    reviewedBy = "Admin"
                                ) { success, message ->
                                    if (success) {
                                        // Notify user about approval
                                        userViewModel.createNotificationForUser(
                                            userId = kyc.userId,
                                            title = "✅ KYC Approved",
                                            message = "Congratulations! Your KYC verification has been approved. You can now access all features.",
                                            type = "system"
                                        ) { _, _ -> }

                                        Toast.makeText(context, "KYC approved", Toast.LENGTH_SHORT)
                                            .show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed: $message",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun KycCard(
    kyc: KycModel,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewImage: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column {
                    Text(
                        kyc.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        kyc.userEmail,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                StatusChip(kyc.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Document Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Document Type",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        kyc.documentType,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        "Submitted",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        formatDate(kyc.submittedAt),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Document Images
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentImagePreview(
                    title = "Front",
                    imageUrl = kyc.frontImageUrl,
                    onClick = { onViewImage(kyc.frontImageUrl) },
                    modifier = Modifier.weight(1f)
                )
                DocumentImagePreview(
                    title = "Back",
                    imageUrl = kyc.backImageUrl,
                    onClick = { onViewImage(kyc.backImageUrl) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (kyc.status == "Rejected" && kyc.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Rejection Reason: ${kyc.rejectionReason}",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }
            
            // Action Buttons
            if (kyc.status == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "Approved" -> Color(0xFF4CAF50) to Color.White
        "Rejected" -> Color(0xFFD32F2F) to Color.White
        else -> Color(0xFFFFA000) to Color.White
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            status,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DocumentImagePreview(
    title: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                .clickable { onClick() },
            contentScale = ContentScale.Crop
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}