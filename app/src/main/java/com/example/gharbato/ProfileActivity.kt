package com.example.gharbato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen()
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF7F00FF)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "My Profile",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
        }


        Card(
            modifier = Modifier
                .offset(y = (-60).dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {

                Image(
                    painter = painterResource(id = R.drawable.billu),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Abhi Khatiwada", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Heroman@gmail.com", color = Color.Gray, fontSize = 14.sp)
                Text("+977 9861996115", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))


                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatItem("12", "Posted", Color(0xFF2979FF))
                    StatItem("8", "Msg Request", Color(0xFFD32F2F))
                    StatItem("4/5", "Rating", Color(0xFF388E3C))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))


//        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
//            Text("Account Settings", color = Color.Gray, fontWeight = FontWeight.SemiBold)
//            Spacer(modifier = Modifier.height(8.dp))

        }
    }
//}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable {}
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF7F00FF))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    ProfileScreen()
}
