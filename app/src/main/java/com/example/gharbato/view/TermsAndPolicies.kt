package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.utils.SystemBarUtils

class TermsAndPoliciesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            TermsAndConditionsScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen() {
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Terms & Conditions",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.DarkGray
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            (context as ComponentActivity).finish()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color.DarkGray,
                    titleContentColor = Color.DarkGray
                ),
                modifier = Modifier.shadow(
                    elevation = 1.dp,
                    spotColor = Color.LightGray
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Welcome Title
                Text(
                    text = "Welcome to GharBato",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Terms and Conditions",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Terms Content with better formatting
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionItem(
                        number = "1",
                        title = "Acceptance of Terms",
                        content = "By accessing or using our services, you agree to comply with these Terms and Conditions, including any future updates."
                    )

                    SectionItem(
                        number = "2",
                        title = "User Registration",
                        content = "Users must register with a valid email address to buy or sell estates through our platform. It is the user's responsibility to maintain the confidentiality of their account information."
                    )

                    SectionItem(
                        number = "3",
                        title = "Estate Listings",
                        content = "All estate listings must be true and accurate. Any fraudulent or misleading listings will result in account suspension or termination."
                    )

                    SectionItem(
                        number = "4",
                        title = "Transactions",
                        content = "The platform facilitates the transaction of estate properties. However, we do not guarantee any specific outcomes in terms of pricing or transaction success."
                    )

                    SectionItem(
                        number = "5",
                        title = "Privacy Policy",
                        content = "We respect your privacy. Any personal information collected will be used solely to provide our services. Refer to our Privacy Policy for more details on how we handle your information."
                    )

                    SectionItem(
                        number = "6",
                        title = "Limitation of Liability",
                        content = "GharBato is not liable for any damages arising from the use of the platform. We provide our services on an 'as-is' basis and do not guarantee specific outcomes or results."
                    )

                    SectionItem(
                        number = "7",
                        title = "Termination of Account",
                        content = "Users who violate our terms and conditions may have their account suspended or terminated at our discretion."
                    )

                    SectionItem(
                        number = "8",
                        title = "Governing Law",
                        content = "These Terms are governed by the laws of the jurisdiction where the estate transaction occurs."
                    )

                    SectionItem(
                        number = "9",
                        title = "Amendments",
                        content = "We reserve the right to amend these terms at any time. Changes will be communicated through the app, and your continued use of the app after changes indicates acceptance of the revised terms."
                    )

                    SectionItem(
                        number = "10",
                        title = "Contact Us",
                        content = "If you have any questions or concerns regarding these terms, feel free to contact us at support@gharbato.com."
                    )

                    // Final Note
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "By using our services, you acknowledge that you have read, understood, and agree to these Terms and Conditions.",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4A4A4A)
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionItem(number: String, title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Blue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            )
        }

        // Content
        Text(
            text = content,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            ),
            modifier = Modifier.padding(start = 36.dp)
        )

        // Divider
        Divider(
            color = Color(0xFFEEEEEE),
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}