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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            HelpAccordion(
                question = "How do I sell a property?",
                answer = "Go to Profile → Sell a Property and fill in property details with images."
            )

            HelpAccordion(
                question = "How do I edit my profile?",
                answer = "Open Profile → Edit Profile. Update your details and tap Save."
            )

            HelpAccordion(
                question = "How can I contact support?",
                answer = "Use the Contact Us option or email support@gharbato.com"
            )

            HelpAccordion(
                question = "Is my data secure?",
                answer = "Yes. Your data is securely stored and never shared without permission."
            )
        }
    }
}

@Composable
fun HelpAccordion(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = !expanded },
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "−" else "+",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7)
                )
            }

            AnimatedVisibility(
                visible = expanded,
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