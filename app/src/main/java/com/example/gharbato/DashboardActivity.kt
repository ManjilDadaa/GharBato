package com.example.gharbato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gharbato.ui.theme.Blue

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DashboardBody()
        }
    }
}

@Composable
fun DashboardBody(){

    var search by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, top = 2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable{

                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Location",
                            style = TextStyle(
                                color = Color.Gray.copy(0.9f)
                            ),
                            modifier = Modifier
                                .padding(start = 5.dp)
                        )
                        Spacer(modifier = Modifier
                            .height(3.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_location_on_24),
                                contentDescription = null,
                                tint = Blue

                            )
                            Text("Kathmandu, Nepal")
                        }

                    }
                }

                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_notifications_24),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp),
                        tint = Blue
                    )
                }

                Spacer(modifier = Modifier
                    .width(6.dp))

                Box(
                    modifier = Modifier
                        .background(color = Color.Gray, shape = CircleShape)

                ){
                    Image(
                        painter = painterResource(R.drawable.baseline_person_24),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable{


                            }
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
                    .height(48.dp),

                value = search,
                onValueChange = { data ->
                    search = data
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Gray.copy(0.2f),
                    unfocusedContainerColor = Color.Gray.copy(0.2f),
                    unfocusedBorderColor = Color.Gray.copy(0.1f),
                    focusedBorderColor = Blue,
                ),
                placeholder = {
                    Text("Search Properties...",
                        style = TextStyle(
                            fontSize = 14.sp
                        )
                    )
                },

                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.outline_search_24),
                        contentDescription = null
                    )
                },

                shape = RoundedCornerShape(10.dp),

            )



         }
    }
}

data class NavItem(
    val title: String,
    val route: String,
    val iconRes: Int
)

val navItems = listOf(
    NavItem("Home", "home", R.drawable.baseline_home_24),
    NavItem("Search", "search", R.drawable.outline_search_24),
    NavItem("Messages", "messages", R.drawable.round_message_24),
    NavItem("Saved", "saved", R.drawable.outline_favorite_border_24),
    NavItem("Profile", "profile", R.drawable.outline_person_24)
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemClick : (String) -> Unit
){
    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(painterResource(item.iconRes), contentDescription = null )
                },
                label = {Text(item.title)}
            )
        }
    }

}

@Composable
fun MainScreen(){
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }

            )
        }
    ){  padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ){
            composable("home"){DashboardBody()}
            composable("search"){DashboardBody()}
            composable ("messages"){DashboardBody()}
            }

    }

}


@Composable
 @Preview
fun DashboardPreview(){
    DashboardBody()
}