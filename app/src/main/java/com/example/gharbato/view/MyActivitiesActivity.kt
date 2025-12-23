package com.example.gharbato.view


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue

class MyActivitiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyActivitiesScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivitiesScreen() {
    val context = LocalContext.current

    // Sample data
    var userRating by remember { mutableStateOf(4.2f) }
    var totalListings by remember { mutableStateOf(0) } // 0 to show empty state
    val recentDevices = listOf("Pixel 6 - Android 13", "iPhone 14 - iOS 16")
    val recentActions = listOf<String>() // empty for demo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Activities") },
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, ProfileScreenActivity::class.java))
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FB))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            MyActivitiesSectionHeader("User Rating")
            UserRatingCard(userRating)

            Spacer(modifier = Modifier.height(16.dp))

            MyActivitiesSectionHeader("Listings Activity")
            if (totalListings == 0) {
                EmptyStateBox(
                    message = "You haven't listed or sold any properties yet.",
                    buttonText = "Add Your First Listing"
                ) {
                    context.startActivity(Intent(context, ListingActivity::class.java))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 20.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$totalListings listings activity graph here", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MyActivitiesSectionHeader("Recent Login Devices")
            if (recentDevices.isEmpty()) {
                Text(
                    "No devices recorded yet.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.Gray
                )
            } else {
                recentDevices.forEach { device ->
                    ActivityItem(label = device)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MyActivitiesSectionHeader("Recent Actions")
            if (recentActions.isEmpty()) {
                Text(
                    "No recent actions yet.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.Gray
                )
            } else {
                recentActions.forEach { action ->
                    ActivityItem(label = action)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/* -------------------- UI COMPONENTS -------------------- */

@Composable
fun MyActivitiesSectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        color = Color(0xFF2C2C2C),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun UserRatingCard(rating: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("Your Rating")
            Spacer(modifier = Modifier.height(8.dp))
            StarRating(rating)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$rating / 5.0", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StarRating(rating: Float) {
    Row {
        val fullStars = rating.toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5f
        for (i in 1..5) {
            when {
                i <= fullStars -> Icon(Icons.Filled.Star, contentDescription = null, tint = Blue, modifier = Modifier.size(24.dp))
                i == fullStars + 1 && hasHalfStar -> Icon(Icons.Filled.StarHalf, contentDescription = null, tint = Blue, modifier = Modifier.size(24.dp))
                else -> Icon(Icons.Filled.StarBorder, contentDescription = null, tint = Blue, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun ActivityItem(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(label, fontSize = 14.sp)
    }
}

@Composable
fun EmptyStateBox(message: String, buttonText: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}
