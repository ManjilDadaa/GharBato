package com.example.gharbato.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.viewmodel.DashboardViewModel
import com.example.gharbato.viewmodel.DashboardViewModelFactory
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.gharbato.utils.SystemBarUtils

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            DashboardBody()
        }
    }
}

class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBody() {
    val context = LocalContext.current
    val activity = context as Activity

    // Hoist PropertyViewModel to Dashboard level so it's shared across screens
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(context)
    )

    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory())
    val unreadCount by dashboardViewModel.unreadMessageCount.collectAsState()
    val latestIncomingMessage by dashboardViewModel.latestIncomingMessage.collectAsState()

    // Bottom NavigationBar data class and its requirements
    data class NavItem(val label: String, val icon: Int)
    var selectedIndex by remember { mutableStateOf(0) }

    val listNav = listOf(
        NavItem("Home", R.drawable.baseline_home_24),
        NavItem("Search", R.drawable.outline_search_24),
        NavItem("Messages", R.drawable.round_message_24),
        NavItem("Saved", R.drawable.outline_favorite_border_24),
        NavItem("Profile", R.drawable.outline_person_24)
    )

    var lastShownMessageId by remember { mutableStateOf<String?>(null) }
    var isMessageOverlayVisible by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(latestIncomingMessage?.id, unreadCount, selectedIndex) {
        val latest = latestIncomingMessage
        if (latest != null && unreadCount > 0 && selectedIndex != 2) {
            if (latest.id != null && latest.id != lastShownMessageId) {
                lastShownMessageId = latest.id
                isMessageOverlayVisible = true
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isMessageOverlayVisible, lastShownMessageId) {
        if (isMessageOverlayVisible) {
            kotlinx.coroutines.delay(3000)
            isMessageOverlayVisible = false
        }
    }

    val incomingCall by CallInvitationManager.incomingCall.collectAsState(initial = null)

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                tonalElevation = 4.dp,
                containerColor = Color.Transparent,
            ) {
                listNav.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            if (item.label == "Messages" && unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(text = unreadCount.toString()) }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = item.label
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = {
                            Text(item.label)
                        },
                        onClick = {
                            selectedIndex = index
                        },
                        selected = selectedIndex == index,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            indicatorColor = Color.Transparent,
                        ),
                        interactionSource = remember { NoRippleInteractionSource() }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when (selectedIndex) {
                0 -> HomeScreen(
                    viewModel = propertyViewModel,
                    onNavigateToSearch = {
                        selectedIndex = 1
                    }
                )
                1 -> SearchScreen(viewModel = propertyViewModel)
                2 -> MessageScreen()
                3 -> SavedScreen(
                    onNavigateToSearch = {
                        selectedIndex = 1
                    }
                )
                4 -> ProfileScreen()
            }

            if (isMessageOverlayVisible && latestIncomingMessage != null && selectedIndex != 2) {
                FloatingMessageOverlay(
                    message = latestIncomingMessage,
                    onClick = {
                        isMessageOverlayVisible = false
                        selectedIndex = 2
                    },
                    onDismiss = {
                        isMessageOverlayVisible = false
                    }
                )
            }

            if (incomingCall != null) {
                FloatingIncomingCallOverlay(
                    call = incomingCall,
                    onAccept = {
                        CallInvitationManager.acceptCurrentCall(activity)
                    },
                    onReject = {
                        CallInvitationManager.rejectCurrentCall()
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingMessageOverlay(
    message: DashboardViewModel.IncomingMessagePreview?,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (message == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 48.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.round_message_24),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = message.senderName,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.text,
                        color = Color(0xFFDDDDDD),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View",
                    color = Blue,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.baseline_close_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
fun FloatingIncomingCallOverlay(
    call: IncomingCall?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    if (call == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF111111),
            shadowElevation = 10.dp
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = call.callerName,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (call.isVideoCall) "Incoming video call" else "Incoming audio call",
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onReject() },
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFB00020)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Reject",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAccept() },
                        shape = RoundedCornerShape(24.dp),
                        color = Blue
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Accept",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DashboardPreview() {
    DashboardBody()
}
