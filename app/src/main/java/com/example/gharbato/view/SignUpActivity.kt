package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.arpitkatiyarprojects.countrypicker.CountryPickerOutlinedTextField
import com.arpitkatiyarprojects.countrypicker.enums.CountryListDisplayType
import com.example.gharbato.R
import com.example.gharbato.model.UserModel
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignUpBody()
        }
    }
}

// Password validation data class
data class PasswordValidation(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false
) {
    val isValid: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
}

// Password validation function
fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 8,
        hasUppercase = password.any { it.isUpperCase() },
        hasLowercase = password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    )
}

// Get max phone length based on country code
fun getMaxPhoneLength(countryCode: String): Int {
    return when (countryCode.lowercase()) {
        "np" -> 10  // Nepal
        "in" -> 10  // India
        "us", "ca" -> 10  // USA, Canada
        "gb" -> 11  // UK
        "au" -> 10  // Australia
        "cn" -> 11  // China
        "br" -> 11  // Brazil
        "de", "fr" -> 10  // Germany, France
        "jp" -> 11  // Japan
        "my" -> 11  // Malaysia
        "sg" -> 8   // Singapore
        "ae" -> 9   // UAE
        "sa" -> 9   // Saudi Arabia
        "pk" -> 10  // Pakistan
        "bd" -> 11  // Bangladesh
        else -> 15  // Default max length
    }
}

@Composable
fun PasswordRequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isMet) R.drawable.baseline_check_24
                else R.drawable.baseline_close_24
            ),
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                color = if (isMet) Color(0xFF4CAF50) else Color.Gray
            )
        )
    }
}

@Composable
fun SignUpBody() {
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }

    var fullname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisibility by remember { mutableStateOf(false) }

    // Password validation states
    var passwordValidation by remember { mutableStateOf(PasswordValidation()) }
    var showPasswordRequirements by remember { mutableStateOf(false) }

    // Phone validation states
    var selectedCountry by remember { mutableStateOf("Nepal") }
    var maxPhoneLength by remember { mutableStateOf(10) }

    val context = LocalContext.current
    val activity = context as Activity

    var terms by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding()
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Create Account",
                        style = TextStyle(
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 20.dp, top = 30.dp)
                    )

                    Text(
                        "Sign up to get started with Ghar Bato",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                    )

                    Text(
                        "Full Name",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        ),
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp)
                    )

                    OutlinedTextField(
                        value = fullname,
                        onValueChange = { data -> fullname = data },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        placeholder = { Text("Enter your full name") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Blue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_person_24),
                                contentDescription = null
                            )
                        }
                    )

                    Text(
                        "Email",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { data -> email = data },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("Enter your email") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Blue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_email_24),
                                contentDescription = null
                            )
                        }
                    )

                    Text(
                        "Phone Number",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )

                    CountryPickerOutlinedTextField(
                        mobileNumber = phoneNo,
                        onMobileNumberChange = { data ->
                            // Only allow digits and restrict to max length
                            val digitsOnly = data.filter { it.isDigit() }
                            if (digitsOnly.length <= maxPhoneLength) {
                                phoneNo = digitsOnly
                            }
                        },
                        countryListDisplayType = CountryListDisplayType.BottomSheet,
                        onCountrySelected = { country ->
                            selectedCountry = country.countryName
                            maxPhoneLength = getMaxPhoneLength(country.countryCode)

                            // Trim phone number if it exceeds new max length
                            if (phoneNo.length > maxPhoneLength) {
                                phoneNo = phoneNo.take(maxPhoneLength)
                            }
                        },
                        defaultCountryCode = "np",
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Blue
                        ),
                        placeholder = {
                            Text("Enter phone number")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(5.dp),
                    )

                    Text(
                        "Password",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { data ->
                            password = data
                            passwordValidation = validatePassword(data)
                            showPasswordRequirements = data.isNotEmpty()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("Enter your password") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = when {
                                password.isEmpty() -> Blue
                                passwordValidation.isValid -> Color(0xFF4CAF50)
                                else -> Color(0xFFE53935)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_lock_24),
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passVisibility = !passVisibility }) {
                                Icon(
                                    painter = painterResource(
                                        if (passVisibility) R.drawable.baseline_visibility_off_24
                                        else R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (!passVisibility)
                            PasswordVisualTransformation()
                        else VisualTransformation.None
                    )

                    // Password requirements indicator
                    if (showPasswordRequirements) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Password must contain:",
                                style = TextStyle(fontSize = 13.sp, color = Color.Gray),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            PasswordRequirementItem("At least 8 characters", passwordValidation.hasMinLength)
                            PasswordRequirementItem("One uppercase letter (A-Z)", passwordValidation.hasUppercase)
                            PasswordRequirementItem("One lowercase letter (a-z)", passwordValidation.hasLowercase)
                            PasswordRequirementItem("One number (0-9)", passwordValidation.hasDigit)
                            PasswordRequirementItem("One special character (!@#$%)", passwordValidation.hasSpecialChar)
                        }
                    }

                    Text(
                        "Confirm Password",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { data -> confirmPassword = data },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = when {
                                password == confirmPassword -> Color(0xFF4CAF50)
                                else -> Blue
                            }
                        ),
                        isError = password != confirmPassword,
                        placeholder = { Text("Confirm your password") },
                        visualTransformation = if (!passVisibility)
                            PasswordVisualTransformation()
                        else VisualTransformation.None,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_lock_24),
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passVisibility = !passVisibility }) {
                                Icon(
                                    painter = painterResource(
                                        if (passVisibility) R.drawable.baseline_visibility_off_24
                                        else R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = terms,
                            onCheckedChange = { terms = !terms },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Blue,
                                uncheckedColor = Gray
                            ),
                            modifier = Modifier.padding(start = 10.dp, end = 5.dp)
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(
                                    style = SpanStyle(
                                        color = Blue,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Terms & Conditions")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val customTabsIntent = CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                customTabsIntent.launchUrl(
                                    context,
                                    "https://bishowdipthapa.com.np/".toUri()
                                )
                            }
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                !terms -> {
                                    Toast.makeText(
                                        context,
                                        "Please agree to Terms and Conditions",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                fullname.isBlank() -> {
                                    Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                }
                                email.isBlank() -> {
                                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                                }
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                    Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                }
                                phoneNo.isBlank() -> {
                                    Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                                }
                                phoneNo.length < 8 -> {
                                    Toast.makeText(context, "Phone number is too short", Toast.LENGTH_SHORT).show()
                                }
                                password.isBlank() -> {
                                    Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                                }
                                !passwordValidation.isValid -> {
                                    Toast.makeText(
                                        context,
                                        "Password does not meet requirements",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                confirmPassword.isBlank() -> {
                                    Toast.makeText(context, "Please confirm your password", Toast.LENGTH_SHORT).show()
                                }
                                password != confirmPassword -> {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    userViewModel.signUp(
                                        email,
                                        password,
                                        fullname,
                                        phoneNo,
                                        selectedCountry
                                    ) { success, message, userId ->
                                        if (success) {
                                            val model = UserModel(
                                                userId = userId,
                                                email = email,
                                                phoneNo = phoneNo,
                                                fullName = fullname,
                                                selectedCountry = selectedCountry
                                            )
                                            userViewModel.addUserToDatabase(
                                                userId,
                                                model
                                            ) { success, message ->
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                            }
                                            val intent = Intent(context, LoginActivity::class.java)
                                            context.startActivity(intent)
                                            activity.finish()
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(message)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = terms,
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Blue,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .size(height = 45.dp, width = 10.dp)
                    ) {
                        Text("Create Account", style = TextStyle(fontSize = 17.sp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            thickness = 2.dp,
                            modifier = Modifier.padding(horizontal = 20.dp).weight(1f)
                        )
                        Text("Or continue with", style = TextStyle(color = Gray))
                        HorizontalDivider(
                            thickness = 2.dp,
                            modifier = Modifier.padding(horizontal = 20.dp).weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 15.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .height(50.dp)
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clickable {},
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
                                Text("Continue with Google", style = TextStyle(fontSize = 15.sp))
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            "Already have an account?",
                            style = TextStyle(fontSize = 15.sp, color = Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            "Sign in",
                            style = TextStyle(fontSize = 15.sp, color = Blue),
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val intent = Intent(context, LoginActivity::class.java)
                                context.startActivity(intent)
                                activity.finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun PreviewSignUp() {
    SignUpBody()
}