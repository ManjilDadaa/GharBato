package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
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

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302b63), Color(0xFF24243e))
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 60.dp, bottom = 40.dp)
        ) {



            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .border(
                            4.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFFFF6584),
                                    Color(0xFF00D9FF)
                                )
                            ),
                            CircleShape
                        )
                        .padding(6.dp)
                ) {
                    Image(
                        painter = if (imageUri != null)
                            rememberAsyncImagePainter(Uri.parse(imageUri))
                        else painterResource(R.drawable.billu),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(email, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(phone, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))


            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                Text("ACCOUNT", fontSize = 11.sp, color = Color.White.copy(0.5f), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))

                GlassMenuItem(
                    R.drawable.baseline_create_24,
                    "Edit Profile",
                    listOf(Color(0xFF667eea), Color(0xFF764ba2))
                ) {
                    context.startActivity(Intent(context, EditProfileActivity::class.java))
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassMenuItem(
                    R.drawable.baseline_watch_24,
                    "My Activities",
                    listOf(Color(0xFFf093fb), Color(0xFFf5576c))
                ) {}

                Spacer(modifier = Modifier.height(12.dp))

                GlassMenuItem(
                    R.drawable.baseline_logout_24,
                    "Logout",
                    listOf(Color(0xFFfa709a), Color(0xFFfee140))
                ) {
                    prefs.edit().clear().apply()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }

                Spacer(modifier = Modifier.height(28.dp))



                Text(
                    "HELP & SUPPORT",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.5f),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassMenuItem(
                    R.drawable.outline_adb_24,
                    "Help Center",
                    listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
                ) {
                    context.startActivity(
                        Intent(context, HelpCenterActivity::class.java)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassMenuItem(
                    R.drawable.baseline_email_24,
                    "Contact Us",
                    listOf(Color(0xFF43e97b), Color(0xFF38f9d7))
                ) {}
            }
        }
    }
}

@Composable
fun GlassMenuItem(
    icon: Int,
    title: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painterResource(R.drawable.outline_arrow_forward_ios_24),
                null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
