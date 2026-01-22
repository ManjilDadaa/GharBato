package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
            tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFAAAAAA),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(fontSize = 12.sp, color = if (isMet) Color(0xFF4CAF50) else Color(0xFFAAAAAA))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedCountryCode by remember { mutableStateOf("977") }
    var selectedCountry by remember { mutableStateOf("Nepal") }
    var maxPhoneLength by remember { mutableStateOf(10) }

    // OTP fields
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }
    var isPhoneVerified by remember { mutableStateOf(false) }

    // Validation states
    var passwordValidation by remember { mutableStateOf(PasswordValidation()) }
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_ios_24),
                            contentDescription = "Back",
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
        ) {
            item {
                // Logo Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gharbato_logo),
                        contentDescription = "Ghar Bato Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .offset(y = 8.dp)
                    )

                    Text(
                        "Create Account",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.DarkGray
                        ),
                        modifier = Modifier.offset(y = (-8).dp)
                    )
                }

                Text(
                    "Sign up to get started with Ghar Bato",
                    style = TextStyle(
                        color = Color(0xFF999999),
                        fontSize = 15.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Full Name
                OutlinedTextField(
                    value = fullname,
                    onValueChange = { fullname = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = { Text("Full name", color = Color(0xFFAAAAAA)) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFEFF6FF),
                        focusedIndicatorColor = Blue,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLeadingIconColor = Blue,
                        unfocusedLeadingIconColor = Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(painterResource(R.drawable.outline_person_24), null) },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    placeholder = { Text("Email address", color = Color(0xFFAAAAAA)) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFEFF6FF),
                        focusedIndicatorColor = Blue,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLeadingIconColor = Blue,
                        unfocusedLeadingIconColor = Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(painterResource(R.drawable.outline_email_24), null) },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number
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
                        selectedCountryCode = country.countryPhoneNumberCode.removePrefix("+")
                        maxPhoneLength = getMaxPhoneLength(country.countryCode)
                        if (phoneNo.length > maxPhoneLength) {
                            phoneNo = phoneNo.take(maxPhoneLength)
                        }
                    },
                    defaultCountryCode = "np",
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFEFF6FF),
                        focusedIndicatorColor = Blue,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Phone number", color = Color(0xFFAAAAAA)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OTP Section
                if (!isPhoneVerified) {
                    if (!isOtpSent) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(56.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = Blue.copy(alpha = 0.3f)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // OTP Input
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                            placeholder = { Text("Enter 6-digit OTP", color = Color(0xFFAAAAAA)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                focusedContainerColor = Color(0xFFEFF6FF),
                                focusedIndicatorColor = Blue,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(56.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = Blue.copy(alpha = 0.3f)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Verify OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Didn't receive code?", color = Color(0xFF999999), fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_check_24),
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Phone Verified",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Fields (only show after phone verified)
                if (isPhoneVerified) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordValidation = validatePassword(it)
                            showPasswordRequirements = it.isNotEmpty()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("Password", color = Color(0xFFAAAAAA)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFEFF6FF),
                            focusedIndicatorColor = when {
                                password.isEmpty() -> Blue
                                passwordValidation.isValid -> Color(0xFF4CAF50)
                                else -> Color(0xFFE53935)
                            },
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLeadingIconColor = Blue,
                            unfocusedLeadingIconColor = Color(0xFFAAAAAA)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        singleLine = true,
                        leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) },
                        trailingIcon = {
                            IconButton(onClick = { passVisibility = !passVisibility }) {
                                Icon(
                                    painterResource(if (passVisibility) R.drawable.baseline_visibility_off_24 else R.drawable.baseline_visibility_24),
                                    null,
                                    tint = Color(0xFFAAAAAA)
                                )
                            }
                        },
                        visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (showPasswordRequirements) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Text("Password must contain:", fontSize = 13.sp, color = Color(0xFF999999), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            PasswordRequirementItem("At least 8 characters", passwordValidation.hasMinLength)
                            PasswordRequirementItem("Uppercase letter (A-Z)", passwordValidation.hasUppercase)
                            PasswordRequirementItem("Lowercase letter (a-z)", passwordValidation.hasLowercase)
                            PasswordRequirementItem("Number (0-9)", passwordValidation.hasDigit)
                            PasswordRequirementItem("Special character (!@#$%)", passwordValidation.hasSpecialChar)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFEFF6FF),
                            focusedIndicatorColor = if (password == confirmPassword && confirmPassword.isNotEmpty()) Color(0xFF4CAF50) else Blue,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLeadingIconColor = Blue,
                            unfocusedLeadingIconColor = Color(0xFFAAAAAA)
                        ),
                        placeholder = { Text("Confirm password", color = Color(0xFFAAAAAA)) },
                        visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        singleLine = true,
                        leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms & Conditions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = terms,
                            onCheckedChange = { terms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Blue,
                                uncheckedColor = Color(0xFFCCCCCC)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(
                                    SpanStyle(
                                        color = Blue,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Terms & Conditions")
                                }
                            },
                            style = TextStyle(fontSize = 14.sp),
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                CustomTabsIntent.Builder().setShowTitle(true).build()
                                    .launchUrl(context, "https://bishowdipthapa.com.np/".toUri())
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Account Button
                    Button(
                        onClick = {
                            when {
                                !terms -> Toast.makeText(context, "Accept Terms", Toast.LENGTH_SHORT).show()
                                fullname.isBlank() -> Toast.makeText(context, "Enter full name", Toast.LENGTH_SHORT).show()
                                email.isBlank() -> Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Toast.makeText(
                                    context,
                                    "Invalid email",
                                    Toast.LENGTH_SHORT
                                ).show()

                                !passwordValidation.isValid -> Toast.makeText(context, "Password requirements not met", Toast.LENGTH_SHORT).show()
                                password != confirmPassword -> Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                                else -> {
                                    isLoading = true
                                    userViewModel.signUp(email, password, fullname, phoneNo, selectedCountry) { success, message, userId ->
                                        if (success) {
                                            val model = UserModel(
                                                userId = userId,
                                                email = email,
                                                phoneNo = "+$selectedCountryCode$phoneNo",
                                                fullName = fullname,
                                                selectedCountry = selectedCountry
                                            )
                                            userViewModel.addUserToDatabase(userId, model) { dbSuccess, dbMessage ->
                                                if (dbSuccess) {
                                                    userViewModel.sendEmailVerification { emailSent, emailMsg ->
                                                        isLoading = false
                                                        if (emailSent) {
                                                            Toast.makeText(
                                                                context,
                                                                "Account created! Please verify your email.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            val intent = Intent(context, EmailVerificationActivity::class.java)
                                                            intent.putExtra("USER_EMAIL", email)
                                                            context.startActivity(intent)
                                                            activity.finish()
                                                        } else {
                                                            Toast.makeText(context, emailMsg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    isLoading = false
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Blue.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue,
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Sign In Card
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        ) {
                            Text(
                                "Already have an account?",
                                style = TextStyle(fontSize = 14.sp, color = Color(0xFF666666))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Sign in",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = Blue,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                        activity.finish()
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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