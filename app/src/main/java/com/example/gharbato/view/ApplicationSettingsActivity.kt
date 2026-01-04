package com.example.gharbato.view

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue

class ApplicationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApplicationSettingsScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSettingsScreen() {
    val context = LocalContext.current


    var isDarkMode by remember { mutableStateOf(false) }

    var selectedLanguage by remember { mutableStateOf("English") }

    var appVersion by remember { mutableStateOf("") }
    val appName = "GharBato"


    LaunchedEffect(Unit) {
        appVersion = getAppVersion(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Application Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Simply finish the activity to go back to Dashboard/Profile
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2C2C2C)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            SettingItem(
                icon = R.drawable.baseline_dark_mode_24,
                title = "Dark Mode",
                subtitle = if (isDarkMode) "Enabled" else "Disabled",
                iconColor = Blue
            ) {
                isDarkMode = !isDarkMode
            }


            SettingItem(
                icon = R.drawable.baseline_language_24,
                title = "Language",
                subtitle = selectedLanguage,
                iconColor = Blue
            ) {
                selectedLanguage = if (selectedLanguage == "English") "Spanish" else "English"
            }


            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "App Info",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )


            SettingItem(
                icon = R.drawable.baseline_info_24,
                title = "App Name",
                subtitle = appName,
                iconColor = Blue
            ) {}

            SettingItem(
                icon = R.drawable.baseline_info_24,
                title = "Version",
                subtitle = appVersion,
                iconColor = Blue
            ) {}

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (e: Exception) {
        "N/A"
    }
}

@Composable
fun SettingItem(
    icon: Int,
    title: String,
    subtitle: String? = "",
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Icon with rounded square background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C2C2C)
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(16.dp)
        )
    }
}