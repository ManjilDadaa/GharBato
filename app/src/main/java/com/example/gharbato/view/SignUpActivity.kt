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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect


class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignUpBody()
        }
    }
}

// Password validation
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

fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 8,
        hasUppercase = password.any { it.isUpperCase() },
        hasLowercase = password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    )
}

fun getMaxPhoneLength(countryCode: String): Int {
    return when (countryCode.lowercase()) {
        "np" -> 10; "in" -> 10; "us", "ca" -> 10; "gb" -> 11
        "au" -> 10; "cn" -> 11; "br" -> 11; "de", "fr" -> 10
        "jp" -> 11; "my" -> 11; "sg" -> 8; "ae" -> 9
        "sa" -> 9; "pk" -> 10; "bd" -> 11
        else -> 15
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
                if (isMet) R.drawable.baseline_check_24 else R.drawable.baseline_close_24
            ),
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(fontSize = 12.sp, color = if (isMet) Color(0xFF4CAF50) else Color.Gray)
        )
    }
}

@Composable
fun SignUpBody() {
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    val context = LocalContext.current
    val activity = context as Activity
    val listState = rememberLazyListState()
    // Basic fields
    var fullname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisibility by remember { mutableStateOf(false) }
    var terms by remember { mutableStateOf(false) }

    // Phone fields
    var phoneNo by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("977") } // Default Nepal
    var selectedCountry by remember { mutableStateOf("Nepal") }
    var maxPhoneLength by remember { mutableStateOf(10) }

    // OTP fields
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }
    var isPhoneVerified by remember { mutableStateOf(false) }

    // Email verification fields
    var isEmailVerificationSent by remember { mutableStateOf(false) }
    var isCheckingEmail by remember { mutableStateOf(false) }

    // Validation states
    var passwordValidation by remember { mutableStateOf(PasswordValidation()) }
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }


    LaunchedEffect(isEmailVerificationSent) {
        if (isEmailVerificationSent) {
            bringIntoViewRequester.bringIntoView()
        }
    }


    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .imePadding()
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // Header
                    Text(
                        "Create Account",
                        style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 20.dp, top = 30.dp)
                    )
                    Text(
                        "Sign up to get started with Ghar Bato",
                        style = TextStyle(color = Color.Gray, fontSize = 16.sp),
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                    )

                    // Full Name
                    Text(
                        "Full Name",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp)
                    )
                    OutlinedTextField(
                        value = fullname,
                        onValueChange = { fullname = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        placeholder = { Text("Enter your full name") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Blue
                        ),
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                        singleLine = true,
                        leadingIcon = { Icon(painterResource(R.drawable.outline_person_24), null) }
                    )

                    // Email
                    Text(
                        "Email",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("Enter your email") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Blue
                        ),
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                        singleLine = true,
                        leadingIcon = { Icon(painterResource(R.drawable.outline_email_24), null) }
                    )

                    // Phone Number
                    Text(
                        "Phone Number",
                        style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                        modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                    )
                    CountryPickerOutlinedTextField(
                        mobileNumber = phoneNo,
                        onMobileNumberChange = { data ->
                            val digitsOnly = data.filter { it.isDigit() }
                            if (digitsOnly.length <= maxPhoneLength) {
                                phoneNo = digitsOnly
                            }
                        },
                        countryListDisplayType = CountryListDisplayType.BottomSheet,
                        onCountrySelected = { country ->
                            selectedCountry = country.countryName
                            // Extract country code from dial code (remove the + prefix)
                            selectedCountryCode = country.countryPhoneNumberCode.removePrefix("+")
                            maxPhoneLength = getMaxPhoneLength(country.countryCode)
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
                        placeholder = { Text("Enter phone number") },
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(5.dp),
                    )

                    // OTP Section
                    if (!isPhoneVerified) {
                        if (!isOtpSent) {
                            // Send OTP Button
                            Button(
                                onClick = {
                                    when {
                                        phoneNo.isBlank() -> Toast.makeText(context, "Enter phone number", Toast.LENGTH_SHORT).show()
                                        phoneNo.length < 8 -> Toast.makeText(context, "Phone number too short", Toast.LENGTH_SHORT).show()
                                        else -> {
                                            isLoading = true
                                            val formattedPhone = "+$selectedCountryCode$phoneNo"

                                            userViewModel.sendOtp(formattedPhone, activity) { success, message, verId ->
                                                isLoading = false
                                                if (success && verId != null) {
                                                    verificationId = verId
                                                    isOtpSent = true
                                                    Toast.makeText(context, "OTP sent to $formattedPhone", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(45.dp),
                                shape = RoundedCornerShape(5.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Send OTP", fontSize = 16.sp)
                                }
                            }
                        } else {
                            // OTP Input
                            Text(
                                "Enter OTP",
                                style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                                modifier = Modifier.padding(start = 20.dp, top = 10.dp)
                            )
                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                                placeholder = { Text("6-digit OTP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Blue
                                ),
                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                                singleLine = true
                            )

                            // Verify OTP Button
                            Button(
                                onClick = {
                                    when {
                                        otp.length != 6 -> Toast.makeText(context, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                                        verificationId == null -> Toast.makeText(context, "Resend OTP", Toast.LENGTH_SHORT).show()
                                        else -> {
                                            isLoading = true
                                            userViewModel.verifyOtp(verificationId!!, otp) { success, message ->
                                                isLoading = false
                                                if (success) {
                                                    isPhoneVerified = true
                                                    Toast.makeText(context, "Phone verified! ✓", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(45.dp),
                                shape = RoundedCornerShape(5.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Verify OTP", fontSize = 16.sp)
                                }
                            }

                            // Resend OTP
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.Center) {
                                Text("Didn't receive code?", color = Color.Gray, fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Resend",
                                    color = Blue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        isOtpSent = false
                                        otp = ""
                                    }
                                )
                            }
                        }
                    } else {
                        // Phone Verified Badge
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(painterResource(R.drawable.baseline_check_24), null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(8.dp))
                            Text("Phone Verified", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                        }
                    }

                    // Password Fields (only show after phone verified)
                    if (isPhoneVerified) {
                        Text(
                            "Password",
                            style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                            modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordValidation = validatePassword(it)
                                showPasswordRequirements = it.isNotEmpty()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            placeholder = { Text("Enter password") },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = when {
                                    password.isEmpty() -> Blue
                                    passwordValidation.isValid -> Color(0xFF4CAF50)
                                    else -> Color(0xFFE53935)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                            singleLine = true,
                            leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) },
                            trailingIcon = {
                                IconButton(onClick = { passVisibility = !passVisibility }) {
                                    Icon(painterResource(if (passVisibility) R.drawable.baseline_visibility_off_24 else R.drawable.baseline_visibility_24), null)
                                }
                            },
                            visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None
                        )

                        if (showPasswordRequirements) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Text("Password must contain:", fontSize = 13.sp, color = Color.Gray)
                                PasswordRequirementItem("At least 8 characters", passwordValidation.hasMinLength)
                                PasswordRequirementItem("Uppercase (A-Z)", passwordValidation.hasUppercase)
                                PasswordRequirementItem("Lowercase (a-z)", passwordValidation.hasLowercase)
                                PasswordRequirementItem("Number (0-9)", passwordValidation.hasDigit)
                                PasswordRequirementItem("Special (!@#$%)", passwordValidation.hasSpecialChar)
                            }
                        }

                        Text(
                            "Confirm Password",
                            style = TextStyle(fontSize = 15.sp, color = Color.DarkGray),
                            modifier = Modifier.padding(start = 20.dp, top = 15.dp)
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = if (password == confirmPassword && confirmPassword.isNotEmpty()) Color(0xFF4CAF50) else Blue
                            ),
                            placeholder = { Text("Confirm password") },
                            visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(56.dp),
                            singleLine = true,
                            leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) }
                        )

                        // Terms & Conditions
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = terms,
                                onCheckedChange = { terms = it },
                                colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Gray),
                                modifier = Modifier.padding(start = 10.dp, end = 5.dp)
                            )
                            Text(
                                buildAnnotatedString {
                                    append("I agree to the ")
                                    withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)) {
                                        append("Terms & Conditions")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, "https://bishowdipthapa.com.np/".toUri())
                                }
                            )
                        }

                        // Create Account Button
                        Button(
                            onClick = {
                                when {
                                    !terms -> Toast.makeText(context, "Accept Terms", Toast.LENGTH_SHORT).show()
                                    fullname.isBlank() -> Toast.makeText(context, "Enter full name", Toast.LENGTH_SHORT).show()
                                    email.isBlank() -> Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
                                    !passwordValidation.isValid -> Toast.makeText(context, "Password requirements not met", Toast.LENGTH_SHORT).show()
                                    password != confirmPassword -> Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        isLoading = true
                                        userViewModel.signUp(email, password, fullname) { success, message, userId ->
                                            if (success) {
                                                val model = UserModel(
                                                    userId = userId,
                                                    email = email,
                                                    phoneNo = "+$selectedCountryCode$phoneNo",
                                                    fullName = fullname,
                                                    selectedCountry = selectedCountry
                                                )
                                                userViewModel.addUserToDatabase(userId, model) { dbSuccess, dbMessage ->
                                                    isLoading = false
                                                    if (dbSuccess) {
                                                        // Send email verification and show verification section
                                                        userViewModel.sendEmailVerification { emailSent, emailMsg ->
                                                            if (emailSent) {
                                                                isEmailVerificationSent = true
                                                                Toast.makeText(context, "Account created! Please verify your email.", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(context, emailMsg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, dbMessage, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                isLoading = false
                                                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = terms && !isLoading,
                            shape = RoundedCornerShape(5.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue,
                                disabledContainerColor = Color.Gray
                            ),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(45.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Create Account", fontSize = 17.sp)
                            }
                        }

                        // Email Verification Section (shown after account creation)
                        if (isEmailVerificationSent) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                                .bringIntoViewRequester(bringIntoViewRequester),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                                Icon(
                                    painterResource(R.drawable.outline_email_24),
                                    null,
                                    tint = Blue,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "Verify Your Email",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "We've sent a verification link to",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    email,
                                    fontSize = 14.sp,
                                    color = Blue,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "Click the link in your email to verify your account",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )

                                Spacer(Modifier.height(24.dp))

                                // Check Verification Button
                                Button(
                                    onClick = {
                                        isCheckingEmail = true
                                        userViewModel.checkEmailVerified { isVerified ->
                                            isCheckingEmail = false
                                            if (isVerified) {
                                                Toast.makeText(context, "Email verified! ✓", Toast.LENGTH_SHORT).show()
                                                context.startActivity(Intent(context, LoginActivity::class.java))
                                                activity.finish()
                                            } else {
                                                Toast.makeText(context, "Email not verified yet. Check your inbox.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isCheckingEmail,
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(5.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                ) {
                                    if (isCheckingEmail) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("I've Verified My Email", fontSize = 16.sp)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Resend Email
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Didn't receive email?", color = Color.Gray, fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Resend",
                                        color = Blue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable {
                                            userViewModel.sendEmailVerification { sent, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                            }
                        }

                        // Already have account (only show if email verification not sent)
                        if (!isEmailVerificationSent) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Already have an account?", fontSize = 15.sp, color = Color.DarkGray)
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "Sign in",
                                    fontSize = 15.sp,
                                    color = Blue,
                                    modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                        activity.finish()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSignUp() {
    SignUpBody()
}