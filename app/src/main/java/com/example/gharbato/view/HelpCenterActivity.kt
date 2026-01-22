package com.example.gharbato.view

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.gharbato.ui.theme.GharBatoTheme

class HelpCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the theme preference
        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

            GharBatoTheme(darkTheme = isDarkMode) {
                HelpCenterScreen()
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen() {
    val activity = LocalContext.current as Activity
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    // State to track which accordion is expanded (null = none expanded)
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
                ),
                title = {
                    Text(
                        "Help Center",
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { activity.finish() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
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
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Frequently Asked Questions",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Find answers to common questions about using GharBato",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // FAQ Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                HelpAccordion(
                    question = "How do I sell a property?",
                    answer = "Go to Profile → Sell a Property and fill in property details with images. Make sure to provide accurate information and high-quality photos to attract potential buyers.",
                    isExpanded = expandedIndex == 0,
                    onToggle = { expandedIndex = if (expandedIndex == 0) null else 0 }
                )

                HelpAccordion(
                    question = "How do I edit my profile?",
                    answer = "Open Profile → Edit Profile. Update your details and tap Save. You can update your contact information, profile picture, and preferences at any time.",
                    isExpanded = expandedIndex == 1,
                    onToggle = { expandedIndex = if (expandedIndex == 1) null else 1 }
                )

                HelpAccordion(
                    question = "How can I contact support?",
                    answer = "Use the Contact Us option in the app or email support@gharbato.com. Our support team typically responds within 24 hours during business days.",
                    isExpanded = expandedIndex == 2,
                    onToggle = { expandedIndex = if (expandedIndex == 2) null else 2 }
                )

                HelpAccordion(
                    question = "Is my data secure?",
                    answer = "Yes. Your data is securely stored and never shared without permission. We use industry-standard encryption and follow strict privacy policies to protect your information.",
                    isExpanded = expandedIndex == 3,
                    onToggle = { expandedIndex = if (expandedIndex == 3) null else 3 }
                )

                HelpAccordion(
                    question = "How do I reset my password?",
                    answer = "Go to Login screen → Forgot Password. Enter your registered email and follow the instructions sent to your inbox to reset your password securely.",
                    isExpanded = expandedIndex == 4,
                    onToggle = { expandedIndex = if (expandedIndex == 4) null else 4 }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Additional Help Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Still need help?",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "If you couldn't find the answer to your question, feel free to contact our support team directly.",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF666666)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HelpAccordion(
    question: String,
    answer: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .background(if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Question Icon
                Icon(
                    painter = painterResource(R.drawable.baseline_help_24),
                    contentDescription = "Question",
                    tint = Blue,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Question Text
                Text(
                    question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black,
                    modifier = Modifier.weight(1f)
                )

                // Expand/Collapse Icon
                Icon(
                    painter = painterResource(
                        if (isExpanded) R.drawable.baseline_remove_24 else R.drawable.baseline_add_24
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Blue,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    answer,
                    fontSize = 14.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}