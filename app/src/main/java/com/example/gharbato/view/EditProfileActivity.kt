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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val lockedFieldBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F7FA)
    val infoBoxBg = if (isDarkMode) Color(0xFF2C3E50).copy(alpha = 0.3f) else Color(0xFFE3F2FD)
    val infoBoxBorder = if (isDarkMode) Color(0xFF3498DB).copy(alpha = 0.3f) else Color(0xFFBBDEFB)

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
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = appBlue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading profile...",
                        color = textColorSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section with Professional Upload
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkMode) 4.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                // Profile Image with enhanced styling
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .shadow(8.dp, CircleShape)
                                ) {
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
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(4.dp, appBlue, CircleShape)
                                            .background(lockedFieldBg),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Camera Icon Button with professional styling
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(4.dp, CircleShape)
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
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Tap camera icon to update photo",
                            fontSize = 13.sp,
                            color = textColorSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Editable Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "EDITABLE INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appBlue,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(
                        modifier = Modifier.weight(2f),
                        color = borderColor,
                        thickness = 1.dp
                    )
                }

                // Form Fields - Editable
                ProfileTextField(
                    value = name,
                    label = "Full Name",
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
                    isEditable = true
                ) { name = it }

                Spacer(modifier = Modifier.height(24.dp))

                // Non-Editable Section with Info Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ACCOUNT INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColorSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(
                        modifier = Modifier.weight(2f),
                        color = borderColor,
                        thickness = 1.dp
                    )
                }

                // Info Box explaining non-editable fields
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = infoBoxBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, infoBoxBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = appBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "These fields cannot be edited for security reasons",
                            fontSize = 12.sp,
                            color = textColorSecondary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }

                ProfileTextField(
                    value = email,
                    label = "Email Address",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
                    isEditable = false
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = phone,
                    label = "Phone Number",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
                    isEditable = false
                ) { }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = country,
                    label = "Country",
                    enabled = false,
                    isDarkMode = isDarkMode,
                    appBlue = appBlue,
                    isEditable = false
                ) { }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button with professional styling
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
                        .height(56.dp)
                        .shadow(
                            elevation = if (isDarkMode) 6.dp else 4.dp,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appBlue,
                        disabledContainerColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFCCCCCC)
                    ),
                    enabled = !isSaving && !isUploadingImage
                ) {
                    if (isSaving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Saving...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Delete Account Button with professional styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDeleteDialog = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = errorColor.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = errorColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Function to generate random 6-character verification code
fun generateRandomCode(): String {
    val chars = ('A'..'Z') + ('0'..'9')
    return List(6) { chars.random() }.joinToString("")
}

// Professional Profile text field with lock icon for non-editable fields
@Composable
fun ProfileTextField(
    value: String,
    label: String,
    enabled: Boolean = true,
    isDarkMode: Boolean,
    appBlue: Color,
    isEditable: Boolean = true,
    onValueChange: (String) -> Unit
) {
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
    val disabledTextColor = if (isDarkMode) Color(0xFF888888) else Color(0xFF757575)
    val borderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
    val focusedBorderColor = appBlue
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val disabledBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F7FA)
    val lockIconColor = if (isDarkMode) Color(0xFF666666) else Color(0xFF999999)

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
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        },
        trailingIcon = if (!isEditable) {
            {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Field is locked",
                    tint = lockIconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            disabledTextColor = disabledTextColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = borderColor,
            disabledBorderColor = borderColor.copy(alpha = 0.5f),
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = disabledBackgroundColor,
            cursorColor = appBlue,
            focusedLabelColor = focusedBorderColor,
            unfocusedLabelColor = if (isDarkMode) Color(0xFF999999) else Color(0xFF666666)
        )
    )
}

// Professional Delete Account Confirmation Dialog
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
    val textColorPrimary = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
    val textColorSecondary = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
    val dialogBackground = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val codeBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFF3E0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Confirm Account Deletion",
                    color = textColorPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "This action is permanent and cannot be undone. To proceed, please enter the verification code below:",
                    color = textColorSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Code display box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = codeBackgroundColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Verification Code",
                            fontSize = 12.sp,
                            color = textColorSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            generatedCode,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = errorColor,
                                letterSpacing = 4.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = onVerificationCodeChange,
                    label = {
                        Text(
                            "Enter the code",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    placeholder = {
                        Text(
                            "XXXXXX",
                            letterSpacing = 2.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appBlue,
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0),
                        focusedTextColor = textColorPrimary,
                        unfocusedTextColor = textColorPrimary,
                        cursorColor = appBlue,
                        errorBorderColor = errorColor
                    ),
                    isError = verificationCode.isNotEmpty() && !isCodeCorrect
                )

                if (verificationCode.isNotEmpty() && !isCodeCorrect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Incorrect code. Please try again.",
                            color = errorColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCodeCorrect) {
                        onAccountDelete()
                    }
                },
                enabled = isCodeCorrect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = errorColor,
                    disabledContainerColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Delete Account",
                    color = if (isCodeCorrect) Color.White else if (isDarkMode) Color(0xFF666666) else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = appBlue
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, appBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Cancel",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = dialogBackground,
        shape = RoundedCornerShape(20.dp)
    )
}