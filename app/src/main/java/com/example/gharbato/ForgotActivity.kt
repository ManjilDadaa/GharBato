package com.example.gharbato

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.IntervalTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue

class ForgotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForgotBody()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotBody() {
    val userRepo = UserRepoImpl()
    var emailError by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current
    Scaffold (topBar ={
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = {
                    val intent =Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                        "Back"

                    )
                }
            }
        )
    }

    ){ innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
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
                color = Color.Gray,
                style = TextStyle(
                    fontSize = 15.sp
                )
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                  unfocusedIndicatorColor = if (emailError) Color.Red else Color.Gray,
                    focusedIndicatorColor = if (emailError) Color.Red else Color.Blue,
                ),
                isError = emailError,
                shape = RoundedCornerShape(10.dp),
                value = email,
                onValueChange = { email = it
                    emailError = !it.endsWith("@gmail.com") },
                placeholder = { Text("Email Address") }
            )
            if (emailError) {
            Text(
                text = "Invalid email, try again.",
                color = Color.Red,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
            Row (
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 30.dp)
            ) {
                Button(
                    onClick = {
                        if (email.isEmpty() || !email.endsWith("@gmail.com")) {
                            emailError = true
                        } else {
                            emailError = false
                            val userRepo = UserRepoImpl()
                            userRepo.forgotPassword(email) { success, message ->
                                (context as? ComponentActivity)?.runOnUiThread {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        // Navigate back to login screen
                                        context.startActivity(Intent(context, MainActivity::class.java))
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    )

                ) {
                    Text("Send Reset Link")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 25.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val annotatedString = buildAnnotatedString {
                    append("Back to ")
                    pushStringAnnotation(tag = "SignIn", annotation = "SignIn")
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = Blue,
                        )
                    ) {
                        append("Sign In")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    style = TextStyle(fontSize = 15.sp, color = Color.Gray),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "SignIn", start = offset, end = offset)
                            .firstOrNull()?.let {
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            }
                    }
                )
            }



        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    ForgotBody()
}