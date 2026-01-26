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
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.ui.theme.GharBatoTheme

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                EditProfileScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

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

    // Theme colors
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)
    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColorPrimary = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val textColorSecondary = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
    val appBlue = if (isDarkMode) Color(0xFF82B1FF) else Color(0xFF4D8DFF)
    val borderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
    val errorColor = if (isDarkMode) Color(0xFFEF5350) else Color(0xFFE53935)

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
            verificationCode = verificationCode,
            isDarkMode = isDarkMode
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        color = textColorPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = appBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
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
                    .padding(padding)
                    .background(backgroundColor)
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
                                .border(3.dp, appBlue, CircleShape)
                                .background(if (isDarkMode) Color(0xFF424242) else Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        // Camera Icon Button for uploading
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(appBlue)
                                .border(3.dp, backgroundColor, CircleShape)
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
                    color = textColorSecondary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Form Fields
                ProfileTextField(
                    value = name,
                    label = "Full Name",
                    isDarkMode = isDarkMode,
                    appBlue = appBlue
                ) { name = it }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = email,
                    label = "Email Address",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = phone,
                    label = "Phone Number",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = country,
                    label = "Country",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue
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
                        containerColor = appBlue,
                        disabledContainerColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFCCCCCC)
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
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Account Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBackgroundColor)
                        .border(
                            width = 1.dp,
                            color = if (isDarkMode) Color(0xFF424242) else Color(0xFFEEEEEE),
                            shape = RoundedCornerShape(12.dp)
                        )
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
                            tint = errorColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = errorColor
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
    isDarkMode: Boolean,
    appBlue: Color,
    onValueChange: (String) -> Unit
) {
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val disabledTextColor = if (isDarkMode) Color(0xFF888888) else Color.Gray
    val borderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color.White

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = if (enabled) {
                    if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
                } else {
                    disabledTextColor
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            disabledTextColor = disabledTextColor,
            focusedBorderColor = appBlue,
            unfocusedBorderColor = borderColor,
            disabledBorderColor = borderColor,
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
            cursorColor = appBlue
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
    verificationCode: String,
    isDarkMode: Boolean
) {
    val isCodeCorrect = verificationCode == generatedCode
    val appBlue = if (isDarkMode) Color(0xFF82B1FF) else Color(0xFF4D8DFF)
    val errorColor = if (isDarkMode) Color(0xFFEF5350) else Color(0xFFE53935)
    val textColorPrimary = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val textColorSecondary = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
    val dialogBackground = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Confirm Account Deletion",
                color = textColorPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "To delete your account, please enter the following code:",
                    color = textColorSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    generatedCode,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = errorColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = onVerificationCodeChange,
                    label = { Text("Enter the code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appBlue,
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0),
                        focusedTextColor = textColorPrimary,
                        unfocusedTextColor = textColorPrimary,
                        cursorColor = appBlue
                    )
                )

                if (verificationCode.isNotEmpty() && !isCodeCorrect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The code you entered is incorrect. Please try again.",
                        color = errorColor,
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
                    color = if (isCodeCorrect) errorColor else if (isDarkMode) Color(0xFF666666) else Color.Gray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = appBlue
                )
            }
        },
        containerColor = dialogBackground
    )
}