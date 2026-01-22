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
import com.example.gharbato.ui.theme.GharBatoTheme

class TermsAndPoliciesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the theme preference
        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

            GharBatoTheme(darkTheme = isDarkMode) {
                TermsAndConditionsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    val goBackToProfile = { (context as ComponentActivity).finish() }

    Scaffold(
        containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
                ),
                title = {
                    Text(
                        "Terms and Conditions",
                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
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
            // Title for Terms and Conditions
            Text(
                text = "Welcome to GharBato Terms and Conditions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Terms Content
            Text(
                text = """
                    1. Acceptance of Terms
                    By accessing or using our services, you agree to comply with these Terms and Conditions, including any future updates.
                    
                    2. User Registration
                    Users must register with a valid email address to buy or sell estates through our platform. It is the user's responsibility to maintain the confidentiality of their account information.
                    
                    3. Estate Listings
                    All estate listings must be true and accurate. Any fraudulent or misleading listings will result in account suspension or termination.
                    
                    4. Transactions
                    The platform facilitates the transaction of estate properties. However, we do not guarantee any specific outcomes in terms of pricing or transaction success.
                    
                    5. Privacy Policy
                    We respect your privacy. Any personal information collected will be used solely to provide our services. Refer to our Privacy Policy for more details on how we handle your information.
                    
                    6. Limitation of Liability
                    GharBato is not liable for any damages arising from the use of the platform. We provide our services on an "as-is" basis and do not guarantee specific outcomes or results.
                    
                    7. Termination of Account
                    Users who violate our terms and conditions may have their account suspended or terminated at our discretion.
                    
                    8. Governing Law
                    These Terms are governed by the laws of the jurisdiction where the estate transaction occurs.
                    
                    9. Amendments
                    We reserve the right to amend these terms at any time. Changes will be communicated through the app, and your continued use of the app after changes indicates acceptance of the revised terms.
                    
                    10. Contact Us
                    If you have any questions or concerns regarding these terms, feel free to contact us at support@gharbato.com.
                    
                    By using our services, you acknowledge that you have read, understood, and agree to these Terms and Conditions.
                """.trimIndent(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
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