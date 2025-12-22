package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R

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
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var email by remember { mutableStateOf(prefs.getString("email", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "") ?: "") }
    var imageUri by remember { mutableStateOf(prefs.getString("profile_image", null)) }

    // Add state for dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }

    // Generate a random code when the activity is created
    val generatedCode = remember { generateRandomCode() }

    // Handle delete account dialog
    if (showDeleteDialog) {
        DeleteAccountConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onAccountDelete = {
                // Handle account deletion logic here
                // For example, clear shared preferences or perform network call
                prefs.edit().clear().apply()
                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()

                // Redirect to LoginActivity after successful deletion
                context.startActivity(Intent(context, LoginActivity::class.java))
                (context as ComponentActivity).finish() // Close the EditProfileActivity
            },
            generatedCode = generatedCode,
            onVerificationCodeChange = { verificationCode = it },
            verificationCode = verificationCode // Current value typed by user
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FB))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Edit UI here...
            ProfileTextField(
                value = name,
                label = "Full Name"
            ) { name = it }

            Spacer(modifier = Modifier.height(14.dp))

            ProfileTextField(
                value = email,
                label = "Email Address"
            ) { email = it }

            Spacer(modifier = Modifier.height(14.dp))

            ProfileTextField(
                value = phone,
                label = "Phone Number"
            ) { phone = it }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    // Save the changes
                    prefs.edit()
                        .putString("name", name)
                        .putString("email", email)
                        .putString("phone", phone)
                        .putString("profile_image", imageUri)
                        .apply()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4D8DFF)
                )
            ) {
                Text(
                    "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Delete Account" Button
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

// Function to generate random 6-character verification code
fun generateRandomCode(): String {
    val chars = ('A'..'Z') + ('0'..'9') + ('!'..'/')
    return List(6) { chars.random() }.joinToString("")
}

// Profile text field
@Composable
fun ProfileTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

// Delete Account Confirmation Dialog
@Composable
fun DeleteAccountConfirmationDialog(
    onDismiss: () -> Unit,
    onAccountDelete: () -> Unit,
    generatedCode: String,
    onVerificationCodeChange: (String) -> Unit,
    verificationCode: String // Current value typed by user
) {
    val isCodeCorrect = verificationCode == generatedCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Account Deletion") },
        text = {
            Column {
                Text("To delete your account, please enter the following code:")

                // Display the generated code using bodyLarge style
                Text(
                    generatedCode,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Input field for verification code
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = onVerificationCodeChange,
                    label = { Text("Enter the code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Display message if the code is incorrect
                if (verificationCode.isNotEmpty() && !isCodeCorrect) {
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
                        onAccountDelete()  // Delete account if the code is correct
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
