package com.example.gharbato.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.ui.theme.Blue

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

    var kycStatus by remember { mutableStateOf("Not Verified") }
    var selectedDoc by remember { mutableStateOf<String?>(null) }
    var trustScore by remember { mutableStateOf(30) } // initial 30%

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trust & Verification", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        kycStatus = "Pending"
                        trustScore = 70 // increase to 70% after submit
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
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
            Text("KYC Status", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                status,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = when (status) {
                    "Verified" -> Color(0xFF2E7D32)
                    "Pending" -> Color(0xFFFFA000)
                    else -> Color(0xFFD32F2F)
                }
            )
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
            Text("Select Document Type", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown logic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                    .clickable { showOptions = !showOptions }
                    .padding(12.dp)
            ) {
                Text(selectedDoc ?: "Select Document Type")
            }

            if (showOptions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                ) {
                    documents.forEach { doc ->
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
                        Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            UploadBox("Upload Front Image", frontImageUri, onFrontClick)
            Spacer(modifier = Modifier.height(8.dp))
            UploadBox("Upload Back Image", backImageUri, onBackClick)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text("Submit KYC", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your KYC will be verified within 48 hours",
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
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(title, color = Color.Gray)
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
            Text("Trust Score: $score%", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Blue
            )

            Spacer(modifier = Modifier.height(12.dp))

            TrustItem("KYC Verified", if(score >= 70) 40 else 0)
            TrustItem("No Reports", 20)
            TrustItem("Email Verified", 10)
            TrustItem("Profile Photo", 15)
            TrustItem("Phone Verified", 15)
        }
    }
}

@Composable
fun TrustItem(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp)
        Text("+$value%", fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
