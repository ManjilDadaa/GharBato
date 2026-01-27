package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
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
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils

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
    val context = LocalContext.current

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue

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
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo
            Image(
                painter = painterResource(R.drawable.gharbato_logo),
                contentDescription = "Ghar Bato Logo",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = 32.dp)
            )

            Spacer(modifier = Modifier.height(-16.dp))

            // Title
            Text(
                "Forgot Password?",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = textColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                "Enter your email address and we'll send you a link to reset your password",
                color = secondaryTextColor,
                style = TextStyle(
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email Input
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5),
                    focusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF6FF),
                    focusedIndicatorColor = primaryColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = Color(0xFFFEF2F2),
                    errorIndicatorColor = Color.Red,
                    focusedLeadingIconColor = primaryColor,
                    unfocusedLeadingIconColor = secondaryTextColor,
                    unfocusedTextColor = textColor,
                    focusedTextColor = textColor
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
                        color = secondaryTextColor
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(com.example.gharbato.R.drawable.baseline_email_24),
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true
            )

            if (emailError) {
                Text(
                    text = "Invalid email, try again.",
                    color = Color.Red,
                    style = TextStyle(fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Send Reset Link Button
            Button(
                onClick = {
                    if (email.isEmpty() || !email.endsWith("@gmail.com")) {
                        emailError = true
                    } else {
                        emailError = false
                        val userRepo = UserRepoImpl()
                        userRepo.forgotPassword(email) { success, message ->
                            (context as? ComponentActivity)?.runOnUiThread {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        spotColor = primaryColor.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Text(
                    "Send Reset Link",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Back to Sign In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            color = primaryColor,
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