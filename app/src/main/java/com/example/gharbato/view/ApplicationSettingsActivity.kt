package com.example.gharbato.view

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
        ThemePreference.init(this) // Your initialization

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode) // Your system bar utility

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

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)
    val cardColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val subtitleColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
    val dividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE)

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
                    IconButton(
                        onClick = {
                            (context as ComponentActivity).finish()
                        }
                    ) {
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
                    spotColor = if (isDarkMode) Color.Transparent else Color.LightGray
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // App Settings Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardColor
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "App Preferences",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )

                    // Dark Mode Setting - Using your ThemePreference system
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ThemePreference.toggleDarkMode(context) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isDarkMode) Blue.copy(alpha = 0.2f) else Color(0x1A4A90E2),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_dark_mode_24),
                                contentDescription = "Dark Mode",
                                tint = Blue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Dark Mode",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = if (isDarkMode) "Enabled" else "Disabled",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = subtitleColor
                                )
                            )
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { ThemePreference.setDarkMode(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Blue,
                                uncheckedThumbColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFF1F1F1),
                                uncheckedTrackColor = if (isDarkMode) Color(0xFF666666) else Color(0xFFCCCCCC)
                            )
                        )
                    }

                    Divider(
                        color = dividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // Language Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLanguage = if (selectedLanguage == "English") "Spanish" else "English"
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isDarkMode) Blue.copy(alpha = 0.2f) else Color(0x1A4A90E2),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_language_24),
                                contentDescription = "Language",
                                tint = Blue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Language",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = selectedLanguage,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = subtitleColor
                                )
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
                            contentDescription = "Arrow",
                            tint = if (isDarkMode) Color(0xFF888888) else Color(0xFFCCCCCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // App Info Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardColor
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "App Information",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )

                    // App Name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isDarkMode) Blue.copy(alpha = 0.2f) else Color(0x1A4A90E2),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_info_24),
                                contentDescription = "App Name",
                                tint = Blue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "App Name",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = appName,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = subtitleColor
                                )
                            )
                        }
                    }

                    Divider(
                        color = dividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // App Version
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isDarkMode) Blue.copy(alpha = 0.2f) else Color(0x1A4A90E2),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_info_24),
                                contentDescription = "Version",
                                tint = Blue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Version",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = appVersion,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = subtitleColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Note
            Text(
                text = "App settings are saved automatically",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF999999)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
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