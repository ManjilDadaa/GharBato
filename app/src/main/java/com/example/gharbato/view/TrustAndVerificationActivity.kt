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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModelProvider
import com.example.gharbato.viewmodel.UserViewModel
import com.example.gharbato.utils.SystemBarUtils

class TrustAndVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            TrustAndVerificationScreen()
        }
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
    var trustScore by remember { mutableStateOf(60) } // Default score with email and phone verified
    var showSuccessDialog by remember { mutableStateOf(false) }
    var hasSubmittedKyc by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }

    var submittedFrontUrl by remember { mutableStateOf<String?>(null) }
    var submittedBackUrl by remember { mutableStateOf<String?>(null) }
    var submittedDocType by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf<String?>(null) }

    // Constants for trust score calculation
    val emailVerified = true // Email verified by default
    val phoneVerified = true // Phone verified by default
    val profilePhoto = false // Profile photo not set by default
    val noReports = true // No reports by default
    val kycVerified = false // KYC not verified by default initially

    // Load KYC data on start
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        val userId = userViewModel.getCurrentUserId()
        if (userId != null) {
            kycViewModel.getUserKycStatus(userId) { kycModel ->
                isLoading = false
                if (kycModel != null) {
                    kycStatus = kycModel.status
                    hasSubmittedKyc = true
                    submittedDocType = kycModel.documentType
                    submittedFrontUrl = kycModel.frontImageUrl
                    submittedBackUrl = kycModel.backImageUrl
                    rejectionReason = kycModel.rejectionReason

                    // Calculate trust score based on status
                    trustScore = calculateTrustScore(
                        kycStatus = kycModel.status,
                        emailVerified = emailVerified,
                        phoneVerified = phoneVerified,
                        profilePhoto = profilePhoto,
                        noReports = noReports
                    )
                } else {
                    kycStatus = "Not Verified"
                    hasSubmittedKyc = false
                    trustScore = calculateTrustScore(
                        kycStatus = "Not Verified",
                        emailVerified = emailVerified,
                        phoneVerified = phoneVerified,
                        profilePhoto = profilePhoto,
                        noReports = noReports
                    )
                }
            }
        } else {
            isLoading = false
            trustScore = calculateTrustScore(
                kycStatus = "Not Verified",
                emailVerified = emailVerified,
                phoneVerified = phoneVerified,
                profilePhoto = profilePhoto,
                noReports = noReports
            )
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
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            title = {
                Text(
                    "KYC Submitted Successfully!",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_check_24),
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your KYC verification request has been submitted.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We'll review your documents and notify you once verification is complete (typically within 24-48 hours).",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trust & Verification",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.DarkGray
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            (context as ComponentActivity).finish()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color.DarkGray,
                    titleContentColor = Color.DarkGray
                ),
                modifier = Modifier.shadow(
                    elevation = 1.dp,
                    spotColor = Color.LightGray
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Blue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading verification data...", color = Color(0xFF666666))
                    }
                }
            } else {
                // KYC Verification Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "KYC Verification",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // KYC Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Status",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF666666)
                                    )
                                )
                                Text(
                                    text = kycStatus,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (kycStatus) {
                                            "Approved" -> Color(0xFF4CAF50)
                                            "Pending" -> Color(0xFFFF9800)
                                            "Rejected" -> Color(0xFFF44336)
                                            else -> Color(0xFF9E9E9E)
                                        }
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (kycStatus) {
                                            "Approved" -> Color(0xFFE8F5E9)
                                            "Pending" -> Color(0xFFFFF3E0)
                                            "Rejected" -> Color(0xFFFDEAEA)
                                            else -> Color(0xFFF5F5F5)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (kycStatus) {
                                            "Approved" -> R.drawable.baseline_check_24
                                            "Pending" -> R.drawable.baseline_access_time_24
                                            "Rejected" -> R.drawable.baseline_close_24
                                            else -> R.drawable.baseline_info_24
                                        }
                                    ),
                                    contentDescription = "Status Icon",
                                    tint = when (kycStatus) {
                                        "Approved" -> Color(0xFF4CAF50)
                                        "Pending" -> Color(0xFFFF9800)
                                        "Rejected" -> Color(0xFFF44336)
                                        else -> Color(0xFF9E9E9E)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Show rejection reason if applicable
                        if (kycStatus == "Rejected" && rejectionReason != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF3CD))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_warning_24),
                                            contentDescription = "Warning",
                                            tint = Color(0xFF856404),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Previous KYC was rejected",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF856404)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Reason: $rejectionReason",
                                        fontSize = 13.sp,
                                        color = Color(0xFF856404)
                                    )
                                }
                            }
                        }

                        // Show either upload form or submitted documents
                        if (!hasSubmittedKyc || kycStatus == "Rejected") {
                            Spacer(modifier = Modifier.height(20.dp))
                            KYCUploadForm(
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
                                                kycViewModel.getUserKycStatus(userId) { kycModel ->
                                                    if (kycModel != null) {
                                                        kycStatus = kycModel.status
                                                        hasSubmittedKyc = true
                                                        submittedDocType = kycModel.documentType
                                                        submittedFrontUrl = kycModel.frontImageUrl
                                                        submittedBackUrl = kycModel.backImageUrl
                                                        rejectionReason = null
                                                        trustScore = calculateTrustScore(
                                                            kycStatus = kycModel.status,
                                                            emailVerified = emailVerified,
                                                            phoneVerified = phoneVerified,
                                                            profilePhoto = profilePhoto,
                                                            noReports = noReports
                                                        )
                                                        showSuccessDialog = true
                                                    }
                                                }
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
                            Spacer(modifier = Modifier.height(20.dp))
                            SubmittedDocumentsCard(
                                documentType = submittedDocType ?: "Unknown",
                                frontImageUrl = submittedFrontUrl,
                                backImageUrl = submittedBackUrl,
                                status = kycStatus
                            )
                        }
                    }
                }

                // Trust Meter Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Trust Score",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Score Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Trust Score",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF666666)
                                    )
                                )
                                Text(
                                    text = "$trustScore%",
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            trustScore >= 80 -> Color(0xFF4CAF50)
                                            trustScore >= 60 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                                )
                            }

                            // Progress Circle
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = trustScore / 100f,
                                    modifier = Modifier.size(80.dp),
                                    color = when {
                                        trustScore >= 80 -> Color(0xFF4CAF50)
                                        trustScore >= 60 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    strokeWidth = 8.dp,
                                    trackColor = Color(0xFFEEEEEE)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Trust Factors
                        Text(
                            text = "Trust Factors",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF666666)
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        TrustFactorItem(
                            title = "KYC Verified",
                            completed = kycStatus == "Approved",
                            points = 40
                        )
                        TrustFactorItem(
                            title = "No Reports",
                            completed = noReports,
                            points = 20
                        )
                        TrustFactorItem(
                            title = "Email Verified",
                            completed = emailVerified,
                            points = 10
                        )
                        TrustFactorItem(
                            title = "Profile Photo",
                            completed = profilePhoto,
                            points = 15
                        )
                        TrustFactorItem(
                            title = "Phone Verified",
                            completed = phoneVerified,
                            points = 15
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "A higher trust score increases credibility and helps build trust with other users.",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF666666),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Helper function to calculate trust score
fun calculateTrustScore(
    kycStatus: String,
    emailVerified: Boolean,
    phoneVerified: Boolean,
    profilePhoto: Boolean,
    noReports: Boolean
): Int {
    var score = 0
    if (kycStatus == "Approved") score += 40
    if (noReports) score += 20
    if (emailVerified) score += 10
    if (profilePhoto) score += 15
    if (phoneVerified) score += 15
    return score
}

@Composable
fun KYCUploadForm(
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

    Column {
        Text(
            text = "Upload Documents",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Document Type Selection
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(
                text = "Document Type",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { showOptions = !showOptions }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDoc ?: "Select document type",
                        color = if (selectedDoc == null) Color(0xFF999999) else Color.Black,
                        fontSize = 15.sp
                    )
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                        contentDescription = "Dropdown",
                        tint = Color(0xFF666666)
                    )
                }
            }

            // Dropdown Options
            if (showOptions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        documents.forEach { doc ->
                            Text(
                                text = doc,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDocChange(doc)
                                        showOptions = false
                                    }
                                    .padding(16.dp),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Image Upload Section
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Upload Images",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UploadImageBox(
                    title = "Front Side",
                    imageUri = frontImageUri,
                    onClick = onFrontClick,
                    modifier = Modifier.weight(1f)
                )
                UploadImageBox(
                    title = "Back Side",
                    imageUri = backImageUri,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = Blue.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue,
                disabledContainerColor = Color(0xFFCCCCCC)
            ),
            enabled = selectedDoc != null && frontImageUri != null && backImageUri != null
        ) {
            Text(
                "Submit KYC Verification",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Info Note
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEFF6FF))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.baseline_info_24),
                    contentDescription = "Info",
                    tint = Blue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Verification typically takes 24-48 hours",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Blue
                    )
                )
            }
        }
    }
}

@Composable
fun UploadImageBox(
    title: String,
    imageUri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = if (imageUri != null) Blue else Color(0xFFDDDDDD),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_photo_alternate_24),
                        contentDescription = "Add Photo",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upload",
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                }
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
    Column {
        Text(
            text = "Submitted Documents",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Document Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Document Type",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            )
            Text(
                text = documentType,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Images Row
        Text(
            text = "Uploaded Images",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SubmittedImageBox(
                label = "Front",
                imageUrl = frontImageUrl,
                modifier = Modifier.weight(1f)
            )
            SubmittedImageBox(
                label = "Back",
                imageUrl = backImageUrl,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Message
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (status) {
                        "Pending" -> Color(0xFFFFF3E0)
                        "Approved" -> Color(0xFFE8F5E9)
                        "Rejected" -> Color(0xFFFDEAEA)
                        else -> Color(0xFFF5F5F5)
                    }
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(
                            when (status) {
                                "Pending" -> R.drawable.baseline_access_time_24
                                "Approved" -> R.drawable.baseline_check_circle_24
                                "Rejected" -> R.drawable.baseline_error_24
                                else -> R.drawable.baseline_info_24
                            }
                        ),
                        contentDescription = "Status Icon",
                        tint = when (status) {
                            "Pending" -> Color(0xFFFF9800)
                            "Approved" -> Color(0xFF4CAF50)
                            "Rejected" -> Color(0xFFF44336)
                            else -> Color(0xFF9E9E9E)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (status) {
                            "Pending" -> "Under Review"
                            "Approved" -> "Verified Successfully"
                            "Rejected" -> "Verification Rejected"
                            else -> "Status Unknown"
                        },
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (status) {
                                "Pending" -> Color(0xFFFF9800)
                                "Approved" -> Color(0xFF4CAF50)
                                "Rejected" -> Color(0xFFF44336)
                                else -> Color(0xFF9E9E9E)
                            }
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (status) {
                        "Pending" -> "Your documents are being reviewed. This usually takes 24-48 hours."
                        "Approved" -> "Your KYC verification has been completed successfully."
                        "Rejected" -> "Your KYC submission was not approved. Please contact support for assistance."
                        else -> "Please check back later for updates."
                    },
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
    }
}

@Composable
fun SubmittedImageBox(
    label: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "$label image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.baseline_image_24),
                    contentDescription = "No Image",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun TrustFactorItem(
    title: String,
    completed: Boolean,
    points: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (completed) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (completed) "✓" else "○",
                    fontSize = 12.sp,
                    color = if (completed) Color(0xFF4CAF50) else Color(0xFFCCCCCC)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = if (completed) FontWeight.Medium else FontWeight.Normal,
                    color = if (completed) Color.Black else Color(0xFF666666)
                )
            )
        }
        Text(
            text = "+$points%",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = if (completed) FontWeight.Bold else FontWeight.Medium,
                color = if (completed) Color(0xFF4CAF50) else Color(0xFFCCCCCC)
            )
        )
    }
}