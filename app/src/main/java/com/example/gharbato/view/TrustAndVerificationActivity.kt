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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModelProvider

class TrustAndVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                TrustAndVerificationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustAndVerificationScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    val userViewModel = remember { UserViewModelProvider.getInstance() }
    val kycViewModel = remember { KycViewModel(KycRepoImpl()) }

    val userData by userViewModel.userData.observeAsState()

    var kycStatus by remember { mutableStateOf("Not Verified") }
    var selectedDoc by remember { mutableStateOf<String?>(null) }
    var trustScore by remember { mutableStateOf(60) }
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
    val emailVerified = true
    val phoneVerified = true
    var profilePhoto by remember { mutableStateOf(false) }
    val noReports = true

    // Update profilePhoto based on userData
    LaunchedEffect(userData) {
        profilePhoto = !userData?.profileImageUrl.isNullOrEmpty() && userData?.profileImageUrl != ""
    }

    // Theme colors
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)
    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColorPrimary = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val textColorSecondary = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
    val appBlue = if (isDarkMode) Color(0xFF82B1FF) else Blue
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFF9800)
    val errorColor = Color(0xFFD32F2F)
    val infoColor = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF2196F3)

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

    // Recalculate trust score when profilePhoto status changes
    LaunchedEffect(profilePhoto, kycStatus) {
        trustScore = calculateTrustScore(
            kycStatus = kycStatus,
            emailVerified = emailVerified,
            phoneVerified = phoneVerified,
            profilePhoto = profilePhoto,
            noReports = noReports
        )
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
                        .background(successColor),
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
                    modifier = Modifier.fillMaxWidth(),
                    color = textColorPrimary
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your KYC verification request has been submitted.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We'll review your documents and notify you once verification is complete (typically within 24-48 hours).",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = successColor
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = appBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it!")
                }
            },
            containerColor = cardBackgroundColor
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trust & Verification",
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = appBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
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
                CircularProgressIndicator(color = appBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // KYC Verification Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackgroundColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkMode) 0.dp else 2.dp
                    )
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
                                color = textColorPrimary
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
                                        color = textColorSecondary
                                    )
                                )
                                Text(
                                    text = kycStatus,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (kycStatus) {
                                            "Approved" -> successColor
                                            "Pending" -> warningColor
                                            "Rejected" -> errorColor
                                            else -> textColorSecondary
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
                                            "Approved" -> successColor.copy(alpha = 0.15f)
                                            "Pending" -> warningColor.copy(alpha = 0.15f)
                                            "Rejected" -> errorColor.copy(alpha = 0.15f)
                                            else -> infoColor.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (kycStatus) {
                                        "Approved" -> "✓"
                                        "Pending" -> "⏱"
                                        "Rejected" -> "✗"
                                        else -> "?"
                                    },
                                    fontSize = 24.sp,
                                    color = when (kycStatus) {
                                        "Approved" -> successColor
                                        "Pending" -> warningColor
                                        "Rejected" -> errorColor
                                        else -> infoColor
                                    }
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
                                    .background(if (isDarkMode) Color(0xFF3A2E1A) else Color(0xFFFFF3CD))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_warning_24),
                                            contentDescription = "Warning",
                                            tint = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Previous KYC was rejected",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Reason: $rejectionReason",
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
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

                                                        userViewModel.createNotification(
                                                            title = "✅ KYC Submitted Successfully",
                                                            message = "Your KYC verification request has been submitted. We'll notify you once it's reviewed (typically within 24-48 hours).",
                                                            type = "system"
                                                        ) { _, _ -> }
                                                    }
                                                }
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
                                onBackClick = { backPicker.launch("image/*") },
                                isDarkMode = isDarkMode,
                                appBlue = appBlue,
                                cardBackgroundColor = cardBackgroundColor
                            )
                        } else {
                            Spacer(modifier = Modifier.height(20.dp))
                            SubmittedDocumentsCard(
                                documentType = submittedDocType ?: "Unknown",
                                frontImageUrl = submittedFrontUrl,
                                backImageUrl = submittedBackUrl,
                                status = kycStatus,
                                isDarkMode = isDarkMode,
                                cardBackgroundColor = cardBackgroundColor
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
                        containerColor = cardBackgroundColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkMode) 0.dp else 2.dp
                    )
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
                                color = textColorPrimary
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
                                        color = textColorSecondary
                                    )
                                )
                                Text(
                                    text = "$trustScore%",
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            trustScore >= 80 -> successColor
                                            trustScore >= 60 -> warningColor
                                            else -> errorColor
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
                                        trustScore >= 80 -> successColor
                                        trustScore >= 60 -> warningColor
                                        else -> errorColor
                                    },
                                    strokeWidth = 8.dp,
                                    trackColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFEEEEEE)
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
                                color = textColorSecondary
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        TrustFactorItem(
                            title = "KYC Verified",
                            completed = kycStatus == "Approved",
                            points = 40,
                            isDarkMode = isDarkMode
                        )
                        TrustFactorItem(
                            title = "No Reports",
                            completed = noReports,
                            points = 20,
                            isDarkMode = isDarkMode
                        )
                        TrustFactorItem(
                            title = "Email Verified",
                            completed = emailVerified,
                            points = 10,
                            isDarkMode = isDarkMode
                        )
                        TrustFactorItem(
                            title = "Profile Photo",
                            completed = profilePhoto,
                            points = 15,
                            isDarkMode = isDarkMode
                        )
                        TrustFactorItem(
                            title = "Phone Verified",
                            completed = phoneVerified,
                            points = 15,
                            isDarkMode = isDarkMode
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF8F9FA))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "A higher trust score increases credibility and helps build trust with other users.",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = textColorSecondary,
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
    onBackClick: () -> Unit,
    isDarkMode: Boolean,
    appBlue: Color,
    cardBackgroundColor: Color
) {
    val documents = listOf("Citizenship", "Driving License", "Passport")
    var showOptions by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Upload Documents",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
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
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5))
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
                        color = if (selectedDoc == null) {
                            if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF999999)
                        } else {
                            if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
                        },
                        fontSize = 15.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = appBlue,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (showOptions) 180f else 0f)
                    )
                }
            }

            // Dropdown Options
            if (showOptions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkMode) 0.dp else 4.dp
                    )
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
                                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
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
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
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
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
                    modifier = Modifier.weight(1f)
                )
                UploadImageBox(
                    title = "Back Side",
                    imageUri = backImageUri,
                    onClick = onBackClick,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
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
                    elevation = if (isDarkMode) 0.dp else 6.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = appBlue.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = appBlue,
                disabledContainerColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFCCCCCC)
            ),
            enabled = selectedDoc != null && frontImageUri != null && backImageUri != null
        ) {
            Text(
                "Submit KYC Verification",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }

        // Info Note
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(appBlue.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.baseline_info_24),
                    contentDescription = "Info",
                    tint = appBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Verification typically takes 24-48 hours",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = appBlue
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
    isDarkMode: Boolean,
    appBlue: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
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
                    color = if (imageUri != null) appBlue else {
                        if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFDDDDDD)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .background(if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)),
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
                        tint = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF999999),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upload",
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF999999)
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
    status: String,
    isDarkMode: Boolean,
    cardBackgroundColor: Color
) {
    Column {
        Text(
            text = "Submitted Documents",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
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
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
                )
            )
            Text(
                text = documentType,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
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
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
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
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            SubmittedImageBox(
                label = "Back",
                imageUrl = backImageUrl,
                isDarkMode = isDarkMode,
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
                        "Pending" -> if (isDarkMode) Color(0xFF3A2E1A) else Color(0xFFFFF3E0)
                        "Approved" -> if (isDarkMode) Color(0xFF1B3A1F) else Color(0xFFE8F5E9)
                        "Rejected" -> if (isDarkMode) Color(0xFF3A1A1F) else Color(0xFFFDEAEA)
                        else -> if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
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
                            else -> if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF666666)
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
                                else -> if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF666666)
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
                        color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
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
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFEEEEEE),
                    RoundedCornerShape(12.dp)
                )
                .background(if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)),
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
                    tint = if (isDarkMode) Color(0xFF666666) else Color(0xFFCCCCCC),
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
    points: Int,
    isDarkMode: Boolean
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
                        if (completed) {
                            if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
                        } else {
                            if (isDarkMode) Color(0xFF424242) else Color(0xFFF5F5F5)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (completed) "✓" else "○",
                    fontSize = 12.sp,
                    color = if (completed) {
                        if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                    } else {
                        if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFFCCCCCC)
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = if (completed) FontWeight.Medium else FontWeight.Normal,
                    color = if (completed) {
                        if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
                    } else {
                        if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
                    }
                )
            )
        }
        Text(
            text = "+$points%",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = if (completed) FontWeight.Bold else FontWeight.Medium,
                color = if (completed) {
                    if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                } else {
                    if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFFCCCCCC)
                }
            )
        )
    }
}