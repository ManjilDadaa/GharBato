package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.viewmodel.UserViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

// Modern color palette
private val PrimaryBlue = Color(0xFF2563EB)
private val DarkBlue = Color(0xFF1D4ED8)
private val LightBlue = Color(0xFF3B82F6)
private val AccentBlue = Color(0xFF60A5FA)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val InputBackground = Color(0xFFF1F5F9)
private val InputFocusedBackground = Color(0xFFEFF6FF)
private val ErrorRed = Color(0xFFEF4444)
private val ErrorBackground = Color(0xFFFEF2F2)
private val DividerColor = Color(0xFFE2E8F0)

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

    // Theme-aware colors
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else BackgroundLight
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else TextPrimary
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else TextSecondary
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else CardWhite
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else PrimaryBlue
    val inputBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputBackground
    val inputFocusBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputFocusedBackground

    // Button animation
    val loginInteractionSource = remember { MutableInteractionSource() }
    val isLoginPressed by loginInteractionSource.collectIsPressedAsState()
    val loginScale by animateFloatAsState(
        targetValue = if (isLoginPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "loginScale"
    )

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
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            item {
                // Header Section with Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gharbato_logo),
                        contentDescription = "Ghar Bato Logo",
                        modifier = Modifier.size(160.dp)
                    )

                    // Welcome Text - close to logo
                    Text(
                        "Welcome Back!",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = textColor
                        ),
                        modifier = Modifier.offset(y = (-32).dp)
                    )

                    Text(
                        "Sign in to continue to GharBato",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        ),
                        modifier = Modifier.offset(y = (-28).dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Login Form Card
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Email Input
                        Text(
                            "Email Address",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

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
                                    "Enter your email",
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = inputBgColor,
                                focusedContainerColor = inputFocusBgColor,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Transparent,
                                errorContainerColor = if (isDarkMode) Color(0xFF3D1414) else ErrorBackground,
                                errorBorderColor = ErrorRed,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedLeadingIconColor = secondaryTextColor,
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .testTag("email_input")
                                .fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Input
                        Text(
                            "Password",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

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
                                    "Enter your password",
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = inputBgColor,
                                focusedContainerColor = inputFocusBgColor,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Transparent,
                                errorContainerColor = if (isDarkMode) Color(0xFF3D1414) else ErrorBackground,
                                errorBorderColor = ErrorRed,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedLeadingIconColor = secondaryTextColor,
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { visibility = !visibility }
                                ) {
                                    Icon(
                                        if (visibility) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (visibility) "Hide password" else "Show password",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            visualTransformation = if (!visibility) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .testTag("password_input")
                                .fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Remember Me & Forgot Password Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Remember Me Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        rememberMe = !rememberMe
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (rememberMe) primaryColor else inputBgColor
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rememberMe) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
                                    color = primaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
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

                        Spacer(modifier = Modifier.height(28.dp))

                        // Login Button
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
                            interactionSource = loginInteractionSource,
                            modifier = Modifier
                                .testTag("login_button")
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(loginScale),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                disabledContainerColor = primaryColor.copy(alpha = 0.6f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    "Sign In",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
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
                        color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else DividerColor,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "or continue with",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else DividerColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign In Button
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    enabled = !isGoogleLoading && !isLoading,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = surfaceColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)
                        )
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.googlee),
                                contentDescription = "Google",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            if (isGoogleLoading) "Signing in..." else "Continue with Google",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Up Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Don't have an account?",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = secondaryTextColor
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            "Sign Up",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = primaryColor,
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
                                .testTag("create_account")
                        )
                    }
                }
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
