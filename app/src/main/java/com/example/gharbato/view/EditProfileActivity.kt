package com.example.gharbato.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
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
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EditProfileScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen() {
    val context = LocalContext.current

    // Initialize ViewModel
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    // Observe user data and update status
    val userData by userViewModel.userData.observeAsState()
    val updateStatus by userViewModel.profileUpdateStatus.observeAsState()
    val uploadedImageUrl by userViewModel.imageUploadStatus.observeAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val generatedCode = remember { generateRandomCode() }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isUploadingImage = true
            userViewModel.uploadProfileImage(context, it)
        }
    }

    // Load user profile when screen opens
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
    }

    // Update local state when userData changes
    LaunchedEffect(userData) {
        userData?.let { user ->
            name = user.fullName
            email = user.email
            phone = user.phoneNo
            country = user.selectedCountry
            profileImageUrl = user.profileImageUrl.ifEmpty { null }
            isLoading = false
        }
    }

    // Handle image upload result
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let { url ->
            isUploadingImage = false
            profileImageUrl = url
            Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle update status
    LaunchedEffect(updateStatus) {
        updateStatus?.let { (success, message) ->
            isSaving = false
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                (context as ComponentActivity).finish()
            }
        }
    }

    // Handle delete account dialog
    if (showDeleteDialog) {
        DeleteAccountConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onAccountDelete = {
                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    } else {
                        Toast.makeText(context, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            generatedCode = generatedCode,
            onVerificationCodeChange = { verificationCode = it },
            verificationCode = verificationCode
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
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
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FB))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section with Upload
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Profile Image
                        Image(
                            painter = if (selectedImageUri != null) {
                                rememberAsyncImagePainter(selectedImageUri)
                            } else if (profileImageUrl != null) {
                                rememberAsyncImagePainter(profileImageUrl)
                            } else {
                                painterResource(R.drawable.billu)
                            },
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF4D8DFF), CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        // Camera Icon Button for uploading
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4D8DFF))
                                .border(3.dp, Color.White, CircleShape)
                                .clickable {
                                    imagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    "Tap to change profile picture",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Form Fields
                ProfileTextField(
                    value = name,
                    label = "Full Name"
                ) { name = it }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = email,
                    label = "Email Address",
                    enabled = false
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = phone,
                    label = "Phone Number",
                    enabled = false
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = country,
                    label = "Country",
                    enabled = false
                ) { }

                Spacer(modifier = Modifier.height(30.dp))

                // Save Button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true

                        // Update profile with or without image
                        if (profileImageUrl != null) {
                            userViewModel.updateUserProfile(name, profileImageUrl!!)
                        } else {
                            userViewModel.updateUserName(name)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4D8DFF)
                    ),
                    enabled = !isSaving && !isUploadingImage
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Account Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            showDeleteDialog = true
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }
        }
    }
}

// Function to generate random 6-character verification code
fun generateRandomCode(): String {
    val chars = ('A'..'Z') + ('0'..'9')
    return List(6) { chars.random() }.joinToString("")
}

// Profile text field
@Composable
fun ProfileTextField(
    value: String,
    label: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color.Gray,
            disabledBorderColor = Color.LightGray,
            disabledLabelColor = Color.Gray
        )
    )
}

// Delete Account Confirmation Dialog
@Composable
fun DeleteAccountConfirmationDialog(
    onDismiss: () -> Unit,
    onAccountDelete: () -> Unit,
    generatedCode: String,
    onVerificationCodeChange: (String) -> Unit,
    verificationCode: String
) {
    val isCodeCorrect = verificationCode == generatedCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Account Deletion") },
        text = {
            Column {
                Text("To delete your account, please enter the following code:")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    generatedCode,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = onVerificationCodeChange,
                    label = { Text("Enter the code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (verificationCode.isNotEmpty() && !isCodeCorrect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The code you entered is incorrect. Please try again.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isCodeCorrect) {
                        onAccountDelete()
                    }
                },
                enabled = isCodeCorrect
            ) {
                Text(
                    "Confirm",
                    color = if (isCodeCorrect) Color(0xFFE53935) else Color.Gray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}