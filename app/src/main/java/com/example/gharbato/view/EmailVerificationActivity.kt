package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.UserViewModel

class EmailVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get email from intent
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        setContent {
            EmailVerificationBody(userEmail)
        }
    }
}

@Composable
fun EmailVerificationBody(userEmail: String) {
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var isCheckingEmail by remember { mutableStateOf(false) }
    var isResendingEmail by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Icon
                    Icon(
                        painter = painterResource(R.drawable.outline_email_24),
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        "Verify Your Email",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                        "We've sent a verification link to",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        userEmail,
                        fontSize = 16.sp,
                        color = Blue,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Instruction Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "📧 Check your email inbox",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                "🔗 Click the verification link",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                "✅ Come back and tap 'I've Verified'",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Check Verification Button
                    Button(
                        onClick = {
                            isCheckingEmail = true
                            userViewModel.checkEmailVerified { isVerified ->
                                isCheckingEmail = false
                                if (isVerified) {
                                    Toast.makeText(
                                        context,
                                        "Email verified successfully! ✓",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Navigate to Dashboard
                                    context.startActivity(
                                        Intent(context, DashboardActivity::class.java)
                                    )
                                    activity?.finish()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Email not verified yet.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = !isCheckingEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue
                        )
                    ) {
                        if (isCheckingEmail) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "I've Verified My Email",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resend Email Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Didn't receive the email?",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isResendingEmail) "Sending..." else "Resend",
                            color = if (isResendingEmail) Color.Gray else Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(enabled = !isResendingEmail) {
                                isResendingEmail = true
                                userViewModel.sendEmailVerification { sent, msg ->
                                    isResendingEmail = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Logout Button
                    TextButton(
                        onClick = {
//                            userViewModel.logout()
                            context.startActivity(
                                Intent(context, LoginActivity::class.java)
                            )
                            activity?.finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Log out and use different account",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}