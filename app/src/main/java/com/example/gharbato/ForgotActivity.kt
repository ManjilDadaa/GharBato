package com.example.gharbato

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ForgotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForgotBody()
        }
    }
}

@Composable
fun ForgotBody() {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, top = 35.dp, end = 20.dp)
        ) {
            // Removed redundant padding parameter usage here
            Icon(
                painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                contentDescription = "Back", // Added contentDescription for accessibility
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "Forgot Password?", style = TextStyle(
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                "Enter your email address and we'll send you a link to reset your password",
                color = Color.Gray,
                style = TextStyle(
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "Email",
                color = Color.Gray, // Changed from colorResource(R.color.Grey) to Color.Gray
                style = TextStyle(
                    fontSize = 15.sp
                )
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Gray,
                    focusedIndicatorColor = Color.Green,
                ),
                shape = RoundedCornerShape(10.dp),
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") }
            )
            Row (
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 30.dp)
            ) {
                Button(
                    onClick = {
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp)

                ) {
                    Text("Send Reset Link")
                }
            }
            Row (modifier = Modifier.fillMaxWidth().padding(start = 140.dp),
            ){
                Text("Back to Sign In", style = TextStyle(
                    color = Color.Gray,
                    fontSize = 15.sp
                ))
            }


        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    ForgotBody()
}