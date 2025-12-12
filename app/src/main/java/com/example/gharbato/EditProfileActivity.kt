package com.example.gharbato

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditProfileScreen()
        }
    }
}

@Composable
fun EditProfileScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var name by remember { mutableStateOf(TextFieldValue(prefs.getString("name", "Abhi Khatiwada")!!)) }
    var email by remember { mutableStateOf(TextFieldValue(prefs.getString("email", "KKKhatiwada@gmail.com")!!)) }
    var phone by remember { mutableStateOf(TextFieldValue(prefs.getString("phone", "+977 9861996115")!!)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Edit Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                val editor = prefs.edit()
                editor.putString("name", name.text)
                editor.putString("email", email.text)
                editor.putString("phone", phone.text)
                editor.apply()

                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
        ) {
            Text("Save Changes", color = Color.White, fontSize = 16.sp)
        }
    }
}
