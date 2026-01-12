package com.example.gharbato.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DashboardBody()
        }
    }
}

class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}

@Composable
fun DashboardBody() {
    val context = LocalContext.current
    val activity = context as Activity

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
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = item.label
                            )
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
                0 -> HomeScreen()
                1 -> SearchScreen()
                2 -> MessageScreen()
                3 -> SavedScreen(
                    onNavigateToSearch = {
                        selectedIndex = 1
                    }
                )
                4 -> ProfileScreen()
            }
        }
    }
}

@Preview
@Composable
fun DashboardPreview() {
    DashboardBody()
}