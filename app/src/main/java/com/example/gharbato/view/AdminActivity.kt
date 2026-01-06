package com.example.gharbato.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdminBody()

        }
    }
}

@Composable
fun AdminBody() {
    data class  navItem(val label: String , val icon : Int)
    var selectedIndex by remember { mutableIntStateOf(0) }
    var listNav = listOf(
        navItem("Home", R.drawable.baseline_home_24),
        navItem("Search", R.drawable.outline_search_24),
        navItem("Report",R.drawable.baseline_report_24),

    )
    Scaffold (
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                tonalElevation = 4.dp,
                containerColor = Color.Transparent,

                ) {
                listNav.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(painter = painterResource(item.icon),
                                contentDescription = null)
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
    ){ padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when(selectedIndex){
                0 -> AdminHomeScreen()
                1 -> AdminSearchScreen()
                2 -> AdminDeleteScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminBodyPreview() {
    AdminBody()
}