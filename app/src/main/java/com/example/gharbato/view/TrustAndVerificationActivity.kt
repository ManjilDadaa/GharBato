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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.UserViewModel

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
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    var kycStatus by remember { mutableStateOf("Not Verified") }
    var selectedDoc by remember { mutableStateOf<String?>(null) }
    var trustScore by remember { mutableStateOf(30) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }

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
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
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
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Blue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FB))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            SectionHeader("KYC Verification")
            KycStatusCard(kycStatus)

            if (kycStatus != "Verified") {
                SectionHeader("Verify your KYC")
                KycUploadCard(
                    selectedDoc = selectedDoc,
                    onDocChange = { selectedDoc = it },
                    onSubmit = {
                        // Update UI state FIRST
                        kycStatus = "Pending"
                        trustScore = 70

                        // Create notification for the user
                        userViewModel.createNotification(
                            title = "✅ KYC Submitted Successfully",
                            message = "Your KYC verification request has been submitted. We'll notify you once it's reviewed (typically within 24-48 hours).",
                            type = "system",
                            imageUrl = "",
                            actionData = ""
                        ) { success, message ->
                            if (success) {
                                showSuccessDialog = true
                                Toast.makeText(
                                    context,
                                    "KYC submitted successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Notification error: $message",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    frontImageUri = frontImageUri,
                    backImageUri = backImageUri,
                    onFrontClick = { frontPicker.launch("image/*") },
                    onBackClick = { backPicker.launch("image/*") }
                )
            }

            SectionHeader("Profile Trust Meter")
            TrustMeterCard(trustScore)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/* -------------------- UI COMPONENTS -------------------- */

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2C2C2C),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
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
        Column {
            Text("KYC Status", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                status,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = when (status) {
                    "Verified" -> Color(0xFF2E7D32)
                    "Pending" -> Color(0xFFFFA000)
                    else -> Color(0xFFD32F2F)
                }
            )
            if (status == "Pending") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your KYC is under review",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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
            Text("Select Document Type", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                    .clickable { showOptions = !showOptions }
                    .padding(12.dp)
            ) {
                Text(
                    selectedDoc ?: "Select Document Type",
                    color = if (selectedDoc == null) Color.Gray else Color.Black
                )
            }

            if (showOptions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
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
                                .padding(12.dp)
                        )
                        if (index < documents.size - 1) {
                            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Upload Document Images", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            UploadBox("Upload Front Image", frontImageUri, onFrontClick)
            Spacer(modifier = Modifier.height(8.dp))
            UploadBox("Upload Back Image", backImageUri, onBackClick)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                enabled = selectedDoc != null && frontImageUri != null && backImageUri != null
            ) {
                Text("Submit KYC", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "📋 Your KYC will be verified within 24-48 hours",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun UploadBox(title: String, imageUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 2.dp,
                color = if (imageUri != null) Blue else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(10.dp)
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
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    title,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun TrustMeterCard(score: Int) {
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
                        score >= 50 -> Color(0xFFFFA000)
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
                    score >= 50 -> Color(0xFFFFA000)
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

            TrustItem("KYC Verified", if (score >= 70) 40 else 0, score >= 70)
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
            Text(
                if (isCompleted) "✓" else "○",
                fontSize = 16.sp,
                color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
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