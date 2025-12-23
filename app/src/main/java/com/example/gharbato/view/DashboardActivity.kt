package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gharbato.MessageScreen
import com.example.gharbato.R
import com.example.gharbato.SavedScreen
import com.example.gharbato.ui.view.SearchScreen

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DashboardBody()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBody(){

    val context = LocalContext.current
    val activity = context as Activity


    // topAppBar scroll behavior
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    //Bottom NavigationBar data class and its requirements
    data class NavItem(val label : String, val icon : Int)
    var selectedIndex by remember { mutableStateOf(0) }

    var listNav = listOf(
        NavItem("Home", R.drawable.baseline_home_24),
        NavItem("Search", R.drawable.outline_search_24),
        NavItem("Messages", R.drawable.round_message_24),
        NavItem("Saved", R.drawable.outline_favorite_border_24),
        NavItem("Person", R.drawable.outline_person_24)
    )
    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.outline_location_on_24),
//                            contentDescription = null,
//                            tint = Blue
//
//                        )
//                        Text("Kathmandu, Nepal",
//                            style = TextStyle(
//                                fontSize = 15.sp
//                            )
//                        )
//                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, ListingActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = null
                        )
                    }
                },

                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.outline_notifications_24),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior

            )
        },
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
                        selected = selectedIndex == index
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when(selectedIndex){
                0 -> HomeScreen()
                1 -> SearchScreen()
                2 -> MessageScreen()
                3 -> SavedScreen()
                4 -> ProfileScreen()
            }
        }
    }
}

@Composable
 @Preview
fun DashboardPreview(){
    DashboardBody()
}