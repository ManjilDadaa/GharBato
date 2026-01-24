package com.example.gharbato.view

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.gharbato.utils.SystemBarUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApplicationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                ApplicationSettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSettingsScreen() {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    var selectedLanguage by remember { mutableStateOf("English") }
    var appVersion by remember { mutableStateOf("") }
    val appName = "GharBato"

    LaunchedEffect(Unit) {
        appVersion = getAppVersion(context)
    }

    val backgroundColor =
        if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Application Settings",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDarkMode)
                                MaterialTheme.colorScheme.onBackground
                            else
                                Color.DarkGray
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = if (isDarkMode)
                                MaterialTheme.colorScheme.onBackground
                            else
                                Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                modifier = Modifier.shadow(
                    elevation = 1.dp,
                    spotColor = Color.LightGray
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp)
        ) {

            SettingItem(
                icon = R.drawable.baseline_dark_mode_24,
                title = "Dark Mode",
                subtitle = if (isDarkMode) "Enabled" else "Disabled",
                iconColor = Blue
            ) {
                ThemePreference.toggleDarkMode(context)
            }

            SettingItem(
                icon = R.drawable.baseline_language_24,
                title = "Language",
                subtitle = selectedLanguage,
                iconColor = Blue
            ) {
                selectedLanguage =
                    if (selectedLanguage == "English") "Spanish" else "English"
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
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
            )
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color.Transparent else Color(0xFFF0F0F0),
                shape = RoundedCornerShape(12.dp)
            )
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
                color = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color(0xFF2C2C2C)
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = if (isDarkMode)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        Color(0xFF999999)
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
            contentDescription = null,
            tint = if (isDarkMode)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else
                Color(0xFFCCCCCC),
            modifier = Modifier.size(16.dp)
        )
    }
}

object ThemePreference {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _isDarkModeState = MutableStateFlow(false)
    val isDarkModeState: StateFlow<Boolean> = _isDarkModeState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkModeState.value = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggleDarkMode(context: Context) {
        val newValue = !_isDarkModeState.value
        _isDarkModeState.value = newValue

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, newValue).apply()
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        _isDarkModeState.value = isDark

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun getDarkModeSync(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }
}