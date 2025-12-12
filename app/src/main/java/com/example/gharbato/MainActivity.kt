package com.example.gharbato

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.ui.theme.Blue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
          LoginBody()
        }
    }
}

@Composable
fun LoginBody(){

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity  = context as Activity

    Scaffold {
        padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ghar Bato",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Blue
                    ),
                    modifier = Modifier
                        .padding(top = 150.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Sign in to continue to Gharbato",
                    style = TextStyle(
                        color = Gray,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text("Email",
                style = TextStyle(
                    color = Color.DarkGray,
                    fontSize = 17.sp
                ),
                modifier = Modifier
                    .padding(start = 20.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { data ->
                    email = data
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                placeholder = {Text("Enter your email",
                    modifier = Modifier.padding(start = 3.dp))},

                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Blue
                ),

                singleLine = true,

                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_email_24),
                        contentDescription = null
                    )
                },

                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .height(60.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Password",
                style = TextStyle(
                    color = Color.DarkGray,
                    fontSize = 17.sp
                ),
                modifier = Modifier
                    .padding(start = 20.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { data ->
                    password = data
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                placeholder = {Text("Enter your password",
                    modifier = Modifier.padding(start = 3.dp))},


                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Blue
                ),

                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_lock_24),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            visibility = !visibility
                        }
                    ) {
                        Icon(
                            painter =
                            if (visibility){
                                painterResource(R.drawable.baseline_visibility_off_24)
                            }
                            else{
                                painterResource(R.drawable.baseline_visibility_24)
                            },
                            contentDescription = null

                        )
                    }
                },

                visualTransformation = if (!visibility){ PasswordVisualTransformation()}
                else {VisualTransformation.None},

                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .height(60.dp)
                    .fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end =20.dp, top = 10.dp)
            ) {
                Text("Forgot Password? ",
                    style = TextStyle(
                        color = Blue
                    ),
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){
                            val intent = Intent(context, ForgotActivity::class.java)
                            context.startActivity(intent)
                        }
                )
            }

            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
                Button(onClick = {
                    val intent = Intent(context, DashboardActivity::class.java)
                    context.startActivity(intent)
                    activity.finish()

                },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                    )

                ) {
                    Text("Log in", style = TextStyle(
                        fontSize = 17.sp
                    ))
                }


            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(thickness = 2.dp,
                    modifier = Modifier.padding(20.dp)
                        .weight(1f)
                )

                Text("Or continue with",
                    style = TextStyle(
                        color = Gray
                    )
                )

                HorizontalDivider(thickness = 2.dp,
                    modifier = Modifier.padding(20.dp)
                        .weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Card(
                    modifier = Modifier
                        .height(50.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clickable{},
                    shape = RoundedCornerShape(15.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.googlee),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("Continue with Google", style = TextStyle(
                            fontSize = 15.sp
                        ))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp)

            ) {
                Text("Dont have an account?",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text("Sign up",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Blue
                    ),
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){
                            val intent = Intent(context, SignUpActivity::class.java)
                            context.startActivity(intent)

                        }
                )
            }


        }
    }
}

@Composable
@Preview
fun PreviewLogin(){
    LoginBody()
}