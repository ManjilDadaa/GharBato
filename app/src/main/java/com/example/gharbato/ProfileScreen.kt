package com.example.gharbato

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ProfileScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfileScreen()
        }
    }
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)


    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        name = prefs.getString("name", "Abhi Khatiwada")!!
        email = prefs.getString("email", "KKKhatiwada@gmail.com")!!
        phone = prefs.getString("phone", "+977 9861996115")!!
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF673AB7), Color(0xFF3F51B5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Image(
                    painter = painterResource(R.drawable.billu),
                    contentDescription = null,
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(email, color = Color.White)
                Text(phone, color = Color.White)

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = { /* SELL property */ },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Sell a Property", color = Color(0xFF673AB7))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "ACCOUNT",
            modifier = Modifier.padding(start = 20.dp),
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )

        SettingItem(
            icon = R.drawable.baseline_create_24,
            title = "Edit Profile",
            onClick = {
                context.startActivity(Intent(context, EditProfileActivity::class.java))
            }
        )

        SettingItem(
            icon = R.drawable.baseline_watch_24,
            title = "My Activitys",
            onClick = { /* Notifications clicked */ }
        )

        SettingItem(
            icon = R.drawable.baseline_logout_24,
            title = "Logout",
            titleColor = Color.Red,
            onClick = { /* Logout clicked */ }
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            "HELP & SUPPORT",
            modifier = Modifier.padding(start = 20.dp),
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )

        SettingItem(
            icon = R.drawable.outline_adb_24,
            title = "Help Center",
            onClick = { /* Help center */ }
        )

        SettingItem(
            icon = R.drawable.baseline_email_24,
            title = "Contact Us",
            onClick = { /* Contact us */ }
        )
    }
}

@Composable
fun SettingItem(
    icon: Int,
    title: String,
    titleColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        Text(
            title,
            color = titleColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(15.dp)
        )
    }
}
