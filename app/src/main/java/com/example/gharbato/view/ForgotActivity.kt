package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.collectAsState
import com.example.gharbato.R
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils

// Modern Color Palette
private val PrimaryBlue = Color(0xFF2563EB)
private val LightBlue = Color(0xFF3B82F6)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val InputBackground = Color(0xFFF1F5F9)
private val InputFocusedBackground = Color(0xFFEFF6FF)
private val ErrorRed = Color(0xFFEF4444)
private val ErrorRedLight = Color(0xFFFEF2F2)

class ForgotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            GharBatoTheme(darkTheme = isDarkMode) {
                ForgotBody(isDarkMode = isDarkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotBody(isDarkMode: Boolean = false) {
    val userRepo = UserRepoImpl()
    var emailError by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else BackgroundLight
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else TextPrimary
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else TextSecondary
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else CardWhite
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else PrimaryBlue
    val inputBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputBackground
    val inputFocusedBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputFocusedBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.gharbato_logo),
                    contentDescription = "Ghar Bato Logo",
                    modifier = Modifier.size(160.dp)
                )

                // Title - close to logo
                Text(
                    "Forgot Password?",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = textColor
                    ),
                    modifier = Modifier.offset(y = (-32).dp)
                )

                // Description
                Text(
                    "Enter your email address and we'll send you\na link to reset your password",
                    color = secondaryTextColor,
                    style = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.offset(y = (-24).dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Form Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = PrimaryBlue.copy(alpha = 0.08f),
                        spotColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    // Email Input
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = inputBgColor,
                            focusedContainerColor = inputFocusedBgColor,
                            focusedBorderColor = if (emailError) ErrorRed else primaryColor,
                            unfocusedBorderColor = if (emailError) ErrorRed.copy(alpha = 0.5f) else Color.Transparent,
                            errorContainerColor = ErrorRedLight,
                            errorBorderColor = ErrorRed,
                            focusedLeadingIconColor = if (emailError) ErrorRed else primaryColor,
                            unfocusedLeadingIconColor = if (emailError) ErrorRed else secondaryTextColor,
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            cursorColor = primaryColor
                        ),
                        isError = emailError,
                        shape = RoundedCornerShape(14.dp),
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError && it.endsWith("@gmail.com")) {
                                emailError = false
                            }
                        },
                        placeholder = {
                            Text(
                                "Email address",
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_email_24),
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true
                    )

                    if (emailError) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_close_24),
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Please enter a valid email address",
                                color = ErrorRed,
                                style = TextStyle(fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Send Reset Link Button
                    val buttonInteractionSource = remember { MutableInteractionSource() }
                    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isButtonPressed) 0.97f else 1f,
                        label = "buttonScale"
                    )

                    Button(
                        onClick = {
                            if (email.isEmpty() || !email.endsWith("@gmail.com")) {
                                emailError = true
                            } else {
                                emailError = false
                                isLoading = true
                                val userRepo = UserRepoImpl()
                                userRepo.forgotPassword(email) { success, message ->
                                    (context as? ComponentActivity)?.runOnUiThread {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Password reset link sent successfully",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        if (success) {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    LoginActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        interactionSource = buttonInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(buttonScale)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = PrimaryBlue.copy(alpha = 0.2f),
                                spotColor = PrimaryBlue.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PrimaryBlue, LightBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Send Reset Link",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Sign In Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else CardWhite
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        "Remember your password?",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Back to Sign In",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val intent = Intent(context, LoginActivity::class.java)
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

@Preview(showBackground = true)
@Composable
fun Preview3() {
    ForgotBody()
}
