package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gharbato.model.GeminiChatMessage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.viewmodel.GeminiChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.gharbato.utils.SystemBarUtils

class GeminiChatActivity : ComponentActivity() {

    companion object {
        fun newIntent(activity: Activity): Intent {
            return Intent(activity, GeminiChatActivity::class.java)
        }
    }

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        val userId = auth.currentUser?.uid ?: "guest"

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            GeminiChatScreen(
                userId = userId,
                onBackClick = { finish() },
                isDarkMode = isDarkMode
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    userId: String,
    onBackClick: () -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val viewModel = remember { GeminiChatViewModel(context, userId) }

    val messages by viewModel.messages
    val messageText by viewModel.messageText
    val isLoading by viewModel.isLoading
    val showWelcome by viewModel.showWelcome

    val listState = rememberLazyListState()

    var menuExpanded by remember { mutableStateOf(false) }

    // Colors based on theme
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val inputBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val aiGradientStart = if (isDarkMode) Color(0xFF6D28D9) else Color(0xFF667EEA)
    val aiGradientEnd = if (isDarkMode) Color(0xFF8B5CF6) else Color(0xFF764BA2)
    val userMessageColor = if (isDarkMode) Color(0xFF0D47A1) else Blue
    val errorBackground = if (isDarkMode) Color(0xFF2D1B1B) else Color(0xFFFFEBEE)
    val errorText = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val borderColor = if (isDarkMode) Color(0xFF424242) else Color.Transparent

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            GeminiChatTopBar(
                onBackClick = onBackClick,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onClearChat = { viewModel.clearConversation() },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                aiGradientStart = aiGradientStart,
                aiGradientEnd = aiGradientEnd
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Welcome message
                if (showWelcome && messages.isEmpty()) {
                    item {
                        WelcomeSection(
                            onQuickMessageClick = { message ->
                                viewModel.sendQuickMessage(message)
                            },
                            isDarkMode = isDarkMode,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            aiGradientStart = aiGradientStart,
                            aiGradientEnd = aiGradientEnd,
                            userMessageColor = userMessageColor
                        )
                    }
                }

                // Messages
                items(messages) { message ->
                    GeminiMessageBubble(
                        message = message,
                        isCurrentUser = message.isFromUser,
                        isDarkMode = isDarkMode,
                        userMessageColor = userMessageColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        errorBackground = errorBackground,
                        errorText = errorText
                    )
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = surfaceColor,
                                modifier = Modifier.widthIn(max = 280.dp),
                                shadowElevation = if (isDarkMode) 2.dp else 2.dp,
                                border = if (isDarkMode) {
                                    CardDefaults.outlinedCardBorder()
                                } else {
                                    null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = userMessageColor
                                    )
                                    Text(
                                        text = "AI is thinking...",
                                        color = secondaryTextColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message Input
            GeminiMessageInput(
                messageText = messageText,
                onMessageTextChange = { viewModel.onMessageTextChanged(it) },
                onSendClick = { viewModel.sendMessage() },
                enabled = !isLoading,
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                inputBackgroundColor = inputBackgroundColor,
                userMessageColor = userMessageColor,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                borderColor = borderColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatTopBar(
    onBackClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    aiGradientStart: Color,
    aiGradientEnd: Color
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    aiGradientStart,
                                    aiGradientEnd
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "AI Assistant",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Powered by Gemini",
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
        },
        actions = {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    Icons.Default.MoreVert,
                    "Menu",
                    tint = textColor
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Clear Conversation",
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        onMenuExpandedChange(false)
                        onClearChat()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = surfaceColor,
            titleContentColor = textColor
        )
    )
}

@Composable
fun WelcomeSection(
    onQuickMessageClick: (String) -> Unit,
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    aiGradientStart: Color,
    aiGradientEnd: Color,
    userMessageColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // AI Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            aiGradientStart,
                            aiGradientEnd
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "Hello! I'm your AI Assistant",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = "I can help you with real estate questions, property advice, and more!",
            fontSize = 14.sp,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Try asking me:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )

        // Quick message cards
        val quickMessages = listOf(
            "What should I look for when buying a house?",
            "How do I calculate home loan EMI?",
            "Tell me about real estate investment",
            "What are the documents needed for property purchase?"
        )

        quickMessages.forEach { message ->
            QuickMessageCard(
                message = message,
                onClick = { onQuickMessageClick(message) },
                isDarkMode = isDarkMode,
                surfaceColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White,
                textColor = userMessageColor
            )
        }
    }
}

@Composable
fun QuickMessageCard(
    message: String,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 2.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GeminiMessageBubble(
    message: GeminiChatMessage,
    isCurrentUser: Boolean,
    isDarkMode: Boolean,
    userMessageColor: Color,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    errorBackground: Color,
    errorText: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = when {
                message.isError -> errorBackground
                isCurrentUser -> userMessageColor
                else -> surfaceColor
            },
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = if (isDarkMode) 2.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isCurrentUser && message.isError) {
                    Text(
                        text = "⚠️ Error",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = errorText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Format the message text with markdown-style formatting
                FormattedText(
                    text = message.text,
                    color = when {
                        message.isError -> errorText
                        isCurrentUser -> Color.White
                        else -> textColor
                    },
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatGeminiTimestamp(message.timestamp),
                    color = when {
                        message.isError -> errorText.copy(alpha = 0.7f)
                        isCurrentUser -> Color.White.copy(alpha = 0.7f)
                        else -> secondaryTextColor
                    },
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FormattedText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    // Parse and format the text
    val parts = parseMarkdown(text)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { part ->
            when (part) {
                is TextPart.Normal -> {
                    if (part.text.isNotBlank()) {
                        Text(
                            text = part.text,
                            color = color,
                            fontSize = fontSize,
                            lineHeight = 20.sp
                        )
                    }
                }
                is TextPart.Bold -> {
                    Text(
                        text = part.text,
                        color = color,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
                is TextPart.BulletPoint -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = color,
                            fontSize = fontSize
                        )
                        Text(
                            text = part.text,
                            color = color,
                            fontSize = fontSize,
                            lineHeight = 20.sp
                        )
                    }
                }
                is TextPart.NumberedPoint -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${part.number}.",
                            color = color,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = part.text,
                            color = color,
                            fontSize = fontSize,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

sealed class TextPart {
    data class Normal(val text: String) : TextPart()
    data class Bold(val text: String) : TextPart()
    data class BulletPoint(val text: String) : TextPart()
    data class NumberedPoint(val number: Int, val text: String) : TextPart()
}

fun parseMarkdown(text: String): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    val lines = text.split("\n")

    for (line in lines) {
        val trimmed = line.trim()

        when {
            // Numbered list: "1. Text" or "1.  Text"
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val number = trimmed.substringBefore(".").toIntOrNull() ?: 1
                val content = trimmed.substringAfter(".").trim()
                val cleanContent = content.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold markers
                parts.add(TextPart.NumberedPoint(number, cleanContent))
            }
            // Bullet point: "* Text" or "- Text"
            trimmed.startsWith("*") && !trimmed.startsWith("**") -> {
                val content = trimmed.substring(1).trim()
                val cleanContent = content.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                parts.add(TextPart.BulletPoint(cleanContent))
            }
            trimmed.startsWith("-") && trimmed.length > 1 -> {
                val content = trimmed.substring(1).trim()
                val cleanContent = content.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                parts.add(TextPart.BulletPoint(cleanContent))
            }
            // Bold text: **Text**
            trimmed.contains(Regex("\\*\\*(.*?)\\*\\*")) -> {
                // Extract bold parts and normal parts
                var remaining = trimmed
                while (remaining.contains("**")) {
                    val beforeBold = remaining.substringBefore("**")
                    if (beforeBold.isNotBlank()) {
                        parts.add(TextPart.Normal(beforeBold))
                    }

                    remaining = remaining.substringAfter("**")
                    if (remaining.contains("**")) {
                        val boldText = remaining.substringBefore("**")
                        if (boldText.isNotBlank()) {
                            parts.add(TextPart.Bold(boldText))
                        }
                        remaining = remaining.substringAfter("**")
                    }
                }
                if (remaining.isNotBlank()) {
                    parts.add(TextPart.Normal(remaining))
                }
            }
            // Normal text
            trimmed.isNotBlank() -> {
                parts.add(TextPart.Normal(trimmed))
            }
        }
    }

    return parts
}

@Composable
fun GeminiMessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean = true,
    isDarkMode: Boolean,
    surfaceColor: Color,
    inputBackgroundColor: Color,
    userMessageColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shadowElevation = if (isDarkMode) 8.dp else 8.dp,
        tonalElevation = if (isDarkMode) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask me anything...",
                        color = secondaryTextColor
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = userMessageColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = inputBackgroundColor,
                    unfocusedContainerColor = inputBackgroundColor,
                    disabledContainerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedPlaceholderColor = secondaryTextColor,
                    unfocusedPlaceholderColor = secondaryTextColor,
                    cursorColor = userMessageColor
                ),
                maxLines = 4,
                enabled = enabled
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (enabled && messageText.isNotBlank()) userMessageColor else
                            if (isDarkMode) Color(0xFF424242) else Color.Gray,
                        CircleShape
                    ),
                enabled = enabled && messageText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatGeminiTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val now = Calendar.getInstance()

    return when {
        isSameDay(calendar, now) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        }
        isYesterday(calendar, now) -> {
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(calendar.time)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val yesterday = cal2.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, yesterday)
}