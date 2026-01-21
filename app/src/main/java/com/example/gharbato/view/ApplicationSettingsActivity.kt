package com.example.gharbato.view

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.gharbato.ui.theme.GharBatoTheme

class ApplicationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode = ThemePreference.isDarkMode(this).collectAsState(initial = false)

            GharBatoTheme(darkTheme = isDarkMode.value) {
                ApplicationSettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSettingsScreen() {
    val context = LocalContext.current
    val isDarkMode = ThemePreference.isDarkMode(context).collectAsState(initial = false)
    var selectedLanguage by remember { mutableStateOf("English") }
    var appVersion by remember { mutableStateOf("") }
    val appName = "GharBato"

    LaunchedEffect(Unit) {
        appVersion = getAppVersion(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Application Settings",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                subtitle = if (isDarkMode.value) "Enabled" else "Disabled",
                iconColor = Blue
            ) {
                ThemePreference.setDarkMode(context, !isDarkMode.value)
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
                color = MaterialTheme.colorScheme.onBackground,
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
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
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
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// Theme Preference Manager
object ThemePreference {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): kotlinx.coroutines.flow.Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            emit(prefs.getBoolean(KEY_DARK_MODE, false))
        }
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun getDarkModeSync(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }
}