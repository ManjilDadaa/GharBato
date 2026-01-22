package com.example.gharbato.view

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
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModelProvider
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.R
import com.example.gharbato.repository.KycRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.KycViewModel
import com.example.gharbato.viewmodel.UserViewModelProvider
import com.example.gharbato.viewmodel.UserViewModel

class TrustAndVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

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

    val appBlue = if (isDarkMode) Color(0xFF82B1FF) else Color(0xFF0061FF)
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)
    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
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

                    trustScore = when (kycModel.status) {
                        "Approved" -> 70
                        "Pending" -> 50
                        "Rejected" -> 30
                        else -> 30
                    }
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
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
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
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
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
                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color(0xFF2C2C2C)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            Icons.Default.ArrowBack,
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
                TrustSectionHeader("KYC Verification", isDarkMode)
                KycStatusCard(kycStatus, isDarkMode, cardBackgroundColor)

                if (!hasSubmittedKyc || kycStatus == "Rejected") {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (kycStatus == "Rejected" && rejectionReason != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0xFF3A2E1A) else Color(0xFFFFF3CD))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    "⚠️ Previous KYC was rejected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
                                )
                                Text(
                                    "Reason: $rejectionReason",
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Please review the reason and resubmit your documents.",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
                                )
                            }
                        }

                    TrustSectionHeader(
                        if (kycStatus == "Rejected") "Resubmit KYC Verification" else "Verify your KYC",
                        isDarkMode
                    )
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
                                        kycViewModel.getUserKycStatus(userId) { kycModel ->
                                            if (kycModel != null) {
                                                kycStatus = kycModel.status
                                                hasSubmittedKyc = true
                                                submittedDocType = kycModel.documentType
                                                submittedFrontUrl = kycModel.frontImageUrl
                                                submittedBackUrl = kycModel.backImageUrl
                                                rejectionReason = null
                                                trustScore = 50
                                                showSuccessDialog = true
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
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
                    Spacer(modifier = Modifier.height(16.dp))
                    TrustSectionHeader("Submitted Documents", isDarkMode)
                    SubmittedDocumentsCard(
                        documentType = submittedDocType ?: "Unknown",
                        frontImageUrl = submittedFrontUrl,
                        backImageUrl = submittedBackUrl,
                        status = kycStatus,
                        isDarkMode = isDarkMode,
                        cardBackgroundColor = cardBackgroundColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                TrustSectionHeader("Profile Trust Meter", isDarkMode)
                TrustMeterCard(trustScore, kycStatus, isDarkMode, cardBackgroundColor)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/* -------------------- UI COMPONENTS -------------------- */

@Composable
fun TrustSectionHeader(title: String, isDarkMode: Boolean) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color(0xFF2C2C2C),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
    )
}

@Composable
fun KycStatusCard(status: String, isDarkMode: Boolean, cardBackgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "KYC Status",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
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
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            "Pending" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                            else -> Color(0xFFD32F2F).copy(alpha = 0.15f)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackgroundColor)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Document Type",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
                Text(
                    documentType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Submitted Images",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubmittedImageBox("Front", frontImageUrl, isDarkMode, Modifier.weight(1f))
                SubmittedImageBox("Back", backImageUrl, isDarkMode, Modifier.weight(1f))
            }

                        Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (status) {
                            "Pending" -> if (isDarkMode) Color(0xFF3A2E1A) else Color(0xFFFFF3CD)
                            "Approved" -> if (isDarkMode) Color(0xFF1B3A1F) else Color(0xFFD4EDDA)
                            "Rejected" -> if (isDarkMode) Color(0xFF3A1A1F) else Color(0xFFF8D7DA)
                            else -> if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FB)
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
                        "Pending" -> if (isDarkMode) Color(0xFFFFB74D) else Color(0xFF856404)
                        "Approved" -> if (isDarkMode) Color(0xFF66BB6A) else Color(0xFF155724)
                        "Rejected" -> if (isDarkMode) Color(0xFFEF5350) else Color(0xFF721C24)
                        else -> if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                    },
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SubmittedImageBox(label: String, imageUrl: String?, isDarkMode: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5))
                .border(
                    1.dp,
                    if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0),
                    RoundedCornerShape(8.dp)
                ),
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
                Text(
                    "No image",
                    fontSize = 12.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
            }
        }
    }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackgroundColor)
            .padding(16.dp)
    ) {
        Column {
            Text(
                "Select Document Type",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFDDDDDD),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { showOptions = !showOptions }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDoc ?: "Choose document type",
                        color = if (selectedDoc == null) {
                            if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                        } else {
                            if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
                        },
                        fontSize = 14.sp
                    )
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
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
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBackgroundColor)
                        .border(
                            1.dp,
                            if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFDDDDDD),
                            RoundedCornerShape(8.dp)
                        )
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
                            fontSize = 14.sp,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
                        )
                        if (index < documents.size - 1) {
                            Divider(
                                color = if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFEEEEEE),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Upload Document Images",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UploadBox("Front Image", frontImageUri, onFrontClick, isDarkMode, appBlue, Modifier.weight(1f))
                UploadBox("Back Image", backImageUri, onBackClick, isDarkMode, appBlue, Modifier.weight(1f))
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = appBlue),
                enabled = selectedDoc != null && frontImageUri != null && backImageUri != null
            ) {
                Text("Submit KYC", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(appBlue.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    "📋 Your KYC will be verified within 24-48 hours",
                    fontSize = 12.sp,
                    color = appBlue
                )
            }
        }
    }
}

@Composable
fun UploadBox(
    title: String,
    imageUri: Uri?,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    appBlue: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
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
                    color = if (imageUri != null) appBlue else {
                        if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFDDDDDD)
                    },
                    shape = RoundedCornerShape(8.dp)
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
                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
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

            TrustItem("KYC Verified", 40, kycStatus == "Approved", isDarkMode)
            TrustItem("No Reports", 20, true, isDarkMode)
            TrustItem("Email Verified", 10, false, isDarkMode)
            TrustItem("Profile Photo", 15, false, isDarkMode)
            TrustItem("Phone Verified", 15, false, isDarkMode)
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
fun TrustItem(label: String, value: Int, isCompleted: Boolean, isDarkMode: Boolean) {
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
                label,
                fontSize = 13.sp,
                color = if (isCompleted) {
                    if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
                } else {
                    if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                }
            )
        }
        Text(
            "+$value%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCompleted) {
                if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
            } else {
                if (isDarkMode) Color(0xFF9E9E9E) else Color.Gray
            }
        )
    }
}