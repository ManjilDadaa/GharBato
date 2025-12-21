package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Black
import com.example.gharbato.ui.theme.Blue

class ProfileScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ProfileScreen() }
    }
}

@Composable
fun ProfileScreen() {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }


    DisposableEffect(Unit) {
        val lifecycleOwner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                name = prefs.getString("name", "Abhi Khatiwada")!!
                email = prefs.getString("email", "KKKhatiwada@gmail.com")!!
                phone = prefs.getString("phone", "+977 9861996115")!!
                imageUri = prefs.getString("profile_image", null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {



        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(color = Blue),
//                .background(
//                    Brush.verticalGradient(
//                        listOf(Color(0xFF6A5AE0), Color(0xFF4D8DFF))
//                    )
//                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Image(
                    painter = if (imageUri != null)
                        rememberAsyncImagePainter(Uri.parse(imageUri))
                    else painterResource(R.drawable.billu),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    email,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Text(
                    phone,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {},
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4D8DFF)
                    ),
                    modifier = Modifier.fillMaxWidth(0.65f)
                ) {
                    Text(
                        "Sell a Property",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))



        SectionTitle("Account")

        ProfileItem(
            icon = R.drawable.baseline_create_24,
            title = "Edit Profile"
        ) {
            context.startActivity(
                Intent(context, EditProfileActivity::class.java)
            )
        }

        ProfileItem(
            icon = R.drawable.baseline_watch_24,
            title = "My Activities"
        ) {}

        ProfileItem(
            icon = R.drawable.baseline_logout_24,
            title = "Logout",
            titleColor = Color.Red
        ) {}

        Spacer(modifier = Modifier.height(24.dp))



        SectionTitle("Help & Support")

        ProfileItem(
            icon = R.drawable.outline_adb_24,
            title = "Help Center"
        ) {}

        ProfileItem(
            icon = R.drawable.baseline_email_24,
            title = "Contact Us"
        ) {}
    }
}



@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        color = Color.Gray,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun ProfileItem(
    icon: Int,
    title: String,
    titleColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F3F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color(0xFF4D8DFF),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
