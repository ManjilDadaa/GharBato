package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.gharbato.R
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.viewmodel.UserViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            GharBatoTheme(darkTheme = isDarkMode) {
                LoginBody(isDarkMode = isDarkMode)
            }
        }
    }
}

// Web Client ID from google-services.json (client_type: 3)
private const val WEB_CLIENT_ID = "1054227769178-furvftv3akb1g2potb97htd6s2ot9kqv.apps.googleusercontent.com"

@Composable
fun LoginBody(isDarkMode: Boolean = false) {
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()

    var isErrorEmail by remember { mutableStateOf(false) }
    var isErrorPassword by remember { mutableStateOf(false) }

    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }

    val adminEmail = "admin@gmail.com"
    val adminPassword = "Admin@123"

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Gray
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue

    // Google Sign-In handler
    fun handleGoogleSignIn() {
        isGoogleLoading = true

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )
                handleGoogleSignInResult(result, userViewModel, context, activity) {
                    isGoogleLoading = false
                }
            } catch (e: GetCredentialException) {
                isGoogleLoading = false
                Log.e("GoogleSignIn", "GetCredentialException: ${e.message}", e)
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            item {
                // Logo Section - More compact
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    // Your GB Logo
                    Image(
                        painter = painterResource(R.drawable.gharbato_logo),
                        contentDescription = "Ghar Bato Logo",
                        modifier = Modifier
                            .size(200.dp)
                            .offset(y = 32.dp)
                    )

                    Text(
                        "Welcome back, Friend!",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = textColor
                        ),
                        modifier = Modifier.offset(y = (-16).dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email Input - Improved styling
                OutlinedTextField(
                    value = email,
                    onValueChange = { data ->
                        email = data
                        if (isErrorEmail) isErrorEmail = false
                    },
                    isError = isErrorEmail,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    placeholder = {
                        Text(
                            "Email address",
                            color = Color(0xFFAAAAAA)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5),
                        focusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF6FF),
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorContainerColor = if (isDarkMode) Color(0xFF3D1414) else Color(0xFFFEF2F2),
                        errorIndicatorColor = Color.Red,
                        focusedLeadingIconColor = primaryColor,
                        unfocusedLeadingIconColor = secondaryTextColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_email_24),
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("email_input")
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input - Improved styling
                OutlinedTextField(
                    value = password,
                    onValueChange = { data ->
                        password = data
                        if (isErrorPassword) isErrorPassword = false
                    },
                    isError = isErrorPassword,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    placeholder = {
                        Text(
                            "Password",
                            color = Color(0xFFAAAAAA)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5),
                        focusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF6FF),
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorContainerColor = if (isDarkMode) Color(0xFF3D1414) else Color(0xFFFEF2F2),
                        errorIndicatorColor = Color.Red,
                        focusedLeadingIconColor = primaryColor,
                        unfocusedLeadingIconColor = secondaryTextColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_lock_24),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                visibility = !visibility
                            }
                        ) {
                            Icon(
                                painter = if (visibility) {
                                    painterResource(R.drawable.baseline_visibility_off_24)
                                } else {
                                    painterResource(R.drawable.baseline_visibility_24)
                                },
                                contentDescription = null,
                                tint = Color(0xFFAAAAAA)
                            )
                        }
                    },
                    visualTransformation = if (!visibility) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("password_input")
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Remember Me & Forgot Password Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Remember Me Checkbox - Improved
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                rememberMe = !rememberMe
                            }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Blue,
                                uncheckedColor = Color(0xFFCCCCCC)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Remember me",
                            style = TextStyle(
                                color = textColor,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Forgot Password
                    Text(
                        "Forgot password?",
                        style = TextStyle(
                            color = Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val intent = Intent(context, ForgotActivity::class.java)
                                context.startActivity(intent)
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button - With better shadow
                Button(
                    onClick = {
                        if(email == adminEmail && password == adminPassword) {
                            val intent = Intent(context, AdminActivity::class.java)
                            context.startActivity(intent)
                            activity.finish()
                        }
                        else {
                            if (email.isEmpty() || password.isEmpty()) {
                                isErrorEmail = email.isEmpty()
                                isErrorPassword = password.isEmpty()
                                Toast.makeText(
                                    context,
                                    "Please enter all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                isLoading = true
                                userViewModel.login(email, password) { success, message ->
                                    if (success) {
                                        userViewModel.checkIsSuspended { isSuspended, reason, until ->
                                            if (isSuspended) {
                                                isLoading = false
                                                userViewModel.logout { _, _ -> }
                                                
                                                val dateStr = if (until != null && until > 0) {
                                                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(until))
                                                } else "Indefinitely"
                                                
                                                val msg = "Account Suspended\nReason: ${reason ?: "Unknown"}\nUntil: $dateStr"
                                                
                                                Toast.makeText(
                                                    context,
                                                    msg,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                // Check if email is verified
                                                userViewModel.checkEmailVerified { isVerified ->
                                                    isLoading = false
                                                    if (isVerified) {
                                                        // Save Remember Me preference
                                                        val sharedPreferences = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                                        with(sharedPreferences.edit()) {
                                                            putBoolean("remember_me", rememberMe)
                                                            apply()
                                                        }

                                                        // Email verified - proceed to dashboard
                                                        Toast.makeText(
                                                            context,
                                                            "Welcome back!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        val intent = Intent(context, DashboardActivity::class.java)
                                                        context.startActivity(intent)
                                                        activity.finish()
                                                    } else {
                                                        // Email not verified - redirect to verification screen
                                                        Toast.makeText(
                                                            context,
                                                            "Please verify your email to continue",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        val intent = Intent(context, EmailVerificationActivity::class.java)
                                                        intent.putExtra("USER_EMAIL", email)
                                                        context.startActivity(intent)
                                                        activity.finish()
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(
                                            context, message, Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .testTag("login_button")
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = Blue.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "Log in",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Divider with "or"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E0E0),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "or",
                        style = TextStyle(
                            color = Color(0xFF999999),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E0E0),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Google Sign In - Improved border
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    enabled = !isGoogleLoading && !isLoading,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = surfaceColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDDDDDD))
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                color = Blue,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.googlee),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            if (isGoogleLoading) "Signing in..." else "Continue with Google",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign Up Section - More prominent
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                    ) {
                        Text(
                            "Don't have an account?",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Create an account",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Blue,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    val intent = Intent(context, SignUpActivity::class.java)
                                    context.startActivity(intent)
                                }
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
@Preview
fun PreviewLogin() {
    LoginBody()
}

/**
 * Handle the Google Sign-In credential result
 */
private fun handleGoogleSignInResult(
    result: GetCredentialResponse,
    userViewModel: UserViewModel,
    context: android.content.Context,
    activity: Activity,
    onComplete: () -> Unit
) {
    when (val credential = result.credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    Log.d("GoogleSignIn", "Got ID Token, authenticating with Firebase...")

                    // Authenticate with Firebase
                    userViewModel.loginWithGoogle(idToken) { success, message, isNewUser ->
                        if (success) {
                            // Check if account is suspended
                            userViewModel.checkIsSuspended { isSuspended, reason, until ->
                                if (isSuspended) {
                                    onComplete()
                                    userViewModel.logout { _, _ -> }

                                    val dateStr = if (until != null && until > 0) {
                                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                            .format(java.util.Date(until))
                                    } else "Indefinitely"

                                    val msg = "Account Suspended\nReason: ${reason ?: "Unknown"}\nUntil: $dateStr"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } else {
                                    // Google accounts are pre-verified, no need to check email verification
                                    onComplete()

                                    // Save Remember Me preference
                                    val sharedPreferences = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                    with(sharedPreferences.edit()) {
                                        putBoolean("remember_me", true)
                                        apply()
                                    }

                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                                    // Navigate to Dashboard
                                    val intent = Intent(context, DashboardActivity::class.java)
                                    context.startActivity(intent)
                                    activity.finish()
                                }
                            }
                        } else {
                            onComplete()
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    onComplete()
                    Log.e("GoogleSignIn", "Invalid Google ID token", e)
                    Toast.makeText(context, "Invalid Google credentials", Toast.LENGTH_SHORT).show()
                }
            } else {
                onComplete()
                Log.e("GoogleSignIn", "Unexpected credential type: ${credential.type}")
                Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
            }
        }
        else -> {
            onComplete()
            Log.e("GoogleSignIn", "Unexpected credential: ${credential::class.java.name}")
            Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
        }
    }
}