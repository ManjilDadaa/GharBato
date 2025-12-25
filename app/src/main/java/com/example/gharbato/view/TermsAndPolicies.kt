package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.gharbato.ui.theme.Blue

class TermsAndPoliciesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TermsAndConditionsScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen() {
    val context = LocalContext.current
    val goBackToProfile = { context.startActivity(Intent(context, ProfileScreenActivity::class.java)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms and Conditions") },
                navigationIcon = {
                    IconButton(onClick = { goBackToProfile() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Blue
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title for Terms and Conditions
            Text(
                text = "Welcome to GharBato Terms and Conditions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
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
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Profile Button
            Button(
                onClick = { goBackToProfile() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text("Go Back to Profile", color = Color.White)
            }
        }
    }
}
