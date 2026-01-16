package com.example.gharbato.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.viewmodel.UserViewModel
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModelProvider

// Updated theme color
private val AppBlue = Color(0xFF0061FF)

class TrustAndVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TrustAndVerificationScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustAndVerificationScreen() {
    val context = LocalContext.current
    val userViewModel = remember { UserViewModelProvider.getInstance() }
    val kycViewModel = remember { KycViewModel(KycRepoImpl()) }

    val userData by userViewModel.userData.observeAsState()

    var kycStatus by remember { mutableStateOf("Not Verified") }
    var selectedDoc by remember { mutableStateOf<String?>(null) }
    var trustScore by remember { mutableStateOf(30) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var hasSubmittedKyc by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }

    var submittedFrontUrl by remember { mutableStateOf<String?>(null) }
    var submittedBackUrl by remember { mutableStateOf<String?>(null) }
    var submittedDocType by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf<String?>(null) } // NEW: Store rejection reason

    // Load KYC data on start
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        val userId = userViewModel.getCurrentUserId()
        if (userId != null) {
            kycViewModel.getUserKycStatus(userId) { kycModel ->
                isLoading = false
                if (kycModel != null) {
                    // User has submitted KYC before
                    kycStatus = kycModel.status
                    hasSubmittedKyc = true
                    submittedDocType = kycModel.documentType
                    submittedFrontUrl = kycModel.frontImageUrl
                    submittedBackUrl = kycModel.backImageUrl
                    rejectionReason = kycModel.rejectionReason // NEW: Get rejection reason

                    trustScore = when (kycModel.status) {
                        "Approved" -> 70  // FIXED: Changed from "Verified" to "Approved"
                        "Pending" -> 50
                        "Rejected" -> 30
                        else -> 30
                    }
                } else {
                    // User has never submitted KYC
                    kycStatus = "Not Verified"
                    hasSubmittedKyc = false
                    trustScore = 30
                }
            }
        } else {
            isLoading = false
        }
    }

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> frontImageUri = uri }
    )
    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> backImageUri = uri }
    )

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    "KYC Submitted Successfully!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your KYC verification request has been submitted.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We'll review your documents and notify you once verification is complete (typically within 24-48 hours).",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4CAF50)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trust & Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FB))
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(8.dp))
                TrustSectionHeader("KYC Verification")
                KycStatusCard(kycStatus)

                // UPDATED LOGIC: Show upload form if never submitted OR if rejected
                if (!hasSubmittedKyc || kycStatus == "Rejected") {
                    // Show upload form
                    Spacer(modifier = Modifier.height(16.dp))

                    // NEW: Show rejection reason if rejected
                    if (kycStatus == "Rejected" && rejectionReason != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF3CD))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    "⚠️ Previous KYC was rejected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF856404)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Reason: $rejectionReason",
                                    fontSize = 13.sp,
                                    color = Color(0xFF856404)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Please review the reason and resubmit your documents.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF856404)
                                )
                            }
                        }
                    }

                    TrustSectionHeader(if (kycStatus == "Rejected") "Resubmit KYC Verification" else "Verify your KYC")
                    KycUploadCard(
                        selectedDoc = selectedDoc,
                        onDocChange = { selectedDoc = it },
                        onSubmit = {
                            val userId = userViewModel.getCurrentUserId()

                            if (userId != null && userData != null && frontImageUri != null && backImageUri != null && selectedDoc != null) {
                                kycViewModel.submitKyc(
                                    userId = userId,
                                    userEmail = userData!!.email,
                                    userName = userData!!.fullName,
                                    documentType = selectedDoc!!,
                                    frontImageUri = frontImageUri!!,
                                    backImageUri = backImageUri!!,
                                    context = context
                                ) { success, message ->
                                    if (success) {
                                        // Reload KYC status after successful submission
                                        kycViewModel.getUserKycStatus(userId) { kycModel ->
                                            if (kycModel != null) {
                                                kycStatus = kycModel.status
                                                hasSubmittedKyc = true
                                                submittedDocType = kycModel.documentType
                                                submittedFrontUrl = kycModel.frontImageUrl
                                                submittedBackUrl = kycModel.backImageUrl
                                                rejectionReason = null // Clear rejection reason
                                                trustScore = 50
                                                showSuccessDialog = true
                                            }
                                        }

                                        userViewModel.createNotification(
                                            title = "✅ KYC Submitted Successfully",
                                            message = "Your KYC verification request has been submitted. We'll notify you once it's reviewed (typically within 24-48 hours).",
                                            type = "system"
                                        ) { _, _ -> }
                                        Toast.makeText(context, "KYC submitted successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to submit KYC: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        frontImageUri = frontImageUri,
                        backImageUri = backImageUri,
                        onFrontClick = { frontPicker.launch("image/*") },
                        onBackClick = { backPicker.launch("image/*") }
                    )
                } else {
                    // Show submitted documents (read-only for Pending or Approved status)
                    Spacer(modifier = Modifier.height(16.dp))
                    TrustSectionHeader("Submitted Documents")
                    SubmittedDocumentsCard(
                        documentType = submittedDocType ?: "Unknown",
                        frontImageUrl = submittedFrontUrl,
                        backImageUrl = submittedBackUrl,
                        status = kycStatus
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                TrustSectionHeader("Profile Trust Meter")
                TrustMeterCard(trustScore, kycStatus)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/* -------------------- UI COMPONENTS -------------------- */

@Composable
fun TrustSectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2C2C2C),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
    )
}

@Composable
fun KycStatusCard(status: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("KYC Status", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = when (status) {
                        "Approved" -> Color(0xFF4CAF50)
                        "Pending" -> Color(0xFFFF9800)
                        "Rejected" -> Color(0xFFD32F2F)
                        else -> Color(0xFFD32F2F)
                    }
                )
                if (status == "Pending") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Under review • 24-48 hours",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            "Pending" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            else -> Color(0xFFD32F2F).copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (status) {
                        "Approved" -> "✓"
                        "Pending" -> "⏱"
                        else -> "✗"
                    },
                    fontSize = 24.sp,
                    color = when (status) {
                        "Approved" -> Color(0xFF4CAF50)
                        "Pending" -> Color(0xFFFF9800)
                        else -> Color(0xFFD32F2F)
                    }
                )
            }
        }
    }
}

@Composable
fun SubmittedDocumentsCard(
    documentType: String,
    frontImageUrl: String?,
    backImageUrl: String?,
    status: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Document Type", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
                Text(documentType, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Submitted Images", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubmittedImageBox("Front", frontImageUrl, Modifier.weight(1f))
                SubmittedImageBox("Back", backImageUrl, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status-specific message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (status) {
                            "Pending" -> Color(0xFFFFF3CD)
                            "Approved" -> Color(0xFFD4EDDA)
                            "Rejected" -> Color(0xFFF8D7DA)
                            else -> Color(0xFFF8F9FB)
                        }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    when (status) {
                        "Pending" -> "ℹ️ Your documents are under review. You'll be notified once verification is complete."
                        "Approved" -> "✅ Your KYC has been verified successfully!"
                        "Rejected" -> "❌ Your KYC was rejected. Please contact support for more information."
                        else -> "ℹ️ Document status unknown."
                    },
                    fontSize = 12.sp,
                    color = when (status) {
                        "Pending" -> Color(0xFF856404)
                        "Approved" -> Color(0xFF155724)
                        "Rejected" -> Color(0xFF721C24)
                        else -> Color.Gray
                    },
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SubmittedImageBox(label: String, imageUrl: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "$label image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No image", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun KycUploadCard(
    selectedDoc: String?,
    onDocChange: (String) -> Unit,
    onSubmit: () -> Unit,
    frontImageUri: Uri?,
    backImageUri: Uri?,
    onFrontClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val documents = listOf("Citizenship", "Driving License", "Passport")
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Text("Select Document Type", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown with arrow icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                    .clickable { showOptions = !showOptions }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDoc ?: "Choose document type",
                        color = if (selectedDoc == null) Color.Gray else Color.Black,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = AppBlue,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (showOptions) 180f else 0f)
                    )
                }
            }

            if (showOptions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                ) {
                    documents.forEachIndexed { index, doc ->
                        Text(
                            doc,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDocChange(doc)
                                    showOptions = false
                                }
                                .padding(12.dp),
                            fontSize = 14.sp
                        )
                        if (index < documents.size - 1) {
                            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Upload Document Images", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UploadBox("Front Image", frontImageUri, onFrontClick, Modifier.weight(1f))
                UploadBox("Back Image", backImageUri, onBackClick, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                enabled = selectedDoc != null && frontImageUri != null && backImageUri != null
            ) {
                Text("Submit KYC", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppBlue.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    "📋 Your KYC will be verified within 24-48 hours",
                    fontSize = 12.sp,
                    color = AppBlue
                )
            }
        }
    }
}

@Composable
fun UploadBox(title: String, imageUri: Uri?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 2.dp,
                    color = if (imageUri != null) AppBlue else Color(0xFFDDDDDD),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onClick() }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Upload",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrustMeterCard(score: Int, kycStatus: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trust Score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "$score%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = when {
                        score >= 70 -> Color(0xFF4CAF50)
                        score >= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFD32F2F)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = when {
                    score >= 70 -> Color(0xFF4CAF50)
                    score >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFD32F2F)
                },
                trackColor = Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Score Breakdown",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            TrustItem("KYC Verified", 40, kycStatus == "Approved")
            TrustItem("No Reports", 20, true)
            TrustItem("Email Verified", 10, false)
            TrustItem("Profile Photo", 15, false)
            TrustItem("Phone Verified", 15, false)
        }
    }
}

@Composable
fun TrustItem(label: String, value: Int, isCompleted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCompleted) "✓" else "○",
                    fontSize = 12.sp,
                    color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                label,
                fontSize = 13.sp,
                color = if (isCompleted) Color.Black else Color.Gray
            )
        }
        Text(
            "+$value%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray
        )
    }
}