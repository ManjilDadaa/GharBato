package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.viewmodel.UserViewModel
import kotlinx.coroutines.launch

// Modern Color Palette
private val PrimaryBlue = Color(0xFF2563EB)
private val DarkBlue = Color(0xFF1D4ED8)
private val LightBlue = Color(0xFF3B82F6)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val InputBackground = Color(0xFFF1F5F9)
private val InputFocusedBackground = Color(0xFFEFF6FF)
private val SuccessGreen = Color(0xFF22C55E)
private val SuccessGreenLight = Color(0xFFDCFCE7)

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            GharBatoTheme(darkTheme = isDarkMode) {
                SignUpBody(isDarkMode = isDarkMode)
            }
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
fun SignUpBody(isDarkMode: Boolean = false) {
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    val context = LocalContext.current
    val activity = context as Activity
    val listState = rememberLazyListState()

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else BackgroundLight
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else TextPrimary
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else TextSecondary
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else CardWhite
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else PrimaryBlue
    val inputBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputBackground
    val inputFocusedBgColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else InputFocusedBackground

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
        containerColor = backgroundColor,
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
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
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
                        .padding(top = 8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gharbato_logo),
                        contentDescription = "Ghar Bato Logo",
                        modifier = Modifier.size(120.dp)
                    )

                    Text(
                        "Create Account",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = textColor
                        ),
                        modifier = Modifier.offset(y = (-16).dp)
                    )

                    Text(
                        "Sign up to get started with GharBato",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.offset(y = (-12).dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Form Card Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = PrimaryBlue.copy(alpha = 0.08f),
                            spotColor = PrimaryBlue.copy(alpha = 0.12f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {

                        // Full Name
                        OutlinedTextField(
                            value = fullname,
                            onValueChange = { fullname = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            placeholder = { Text("Full name", color = TextSecondary.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = inputBgColor,
                                focusedContainerColor = inputFocusedBgColor,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedLeadingIconColor = secondaryTextColor,
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(painterResource(R.drawable.outline_person_24), null) },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            placeholder = { Text("Email address", color = TextSecondary.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = inputBgColor,
                                focusedContainerColor = inputFocusedBgColor,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedLeadingIconColor = secondaryTextColor,
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(painterResource(R.drawable.outline_email_24), null) },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                unfocusedContainerColor = inputBgColor,
                                focusedContainerColor = inputFocusedBgColor,
                                focusedIndicatorColor = primaryColor,
                                unfocusedIndicatorColor = Color.Transparent,
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor
                            ),
                            placeholder = { Text("Phone number", color = TextSecondary.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // OTP Section
                        if (!isPhoneVerified) {
                            if (!isOtpSent) {
                                val sendOtpInteractionSource = remember { MutableInteractionSource() }
                                val isSendOtpPressed by sendOtpInteractionSource.collectIsPressedAsState()
                                val sendOtpScale by animateFloatAsState(
                                    targetValue = if (isSendOtpPressed) 0.97f else 1f,
                                    label = "sendOtpScale"
                                )

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
                                    interactionSource = sendOtpInteractionSource,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .scale(sendOtpScale)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            ambientColor = PrimaryBlue.copy(alpha = 0.2f),
                                            spotColor = PrimaryBlue.copy(alpha = 0.3f)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(PrimaryBlue, LightBlue)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                // OTP Input
                                OutlinedTextField(
                                    value = otp,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                                    placeholder = { Text("Enter 6-digit OTP", color = TextSecondary.copy(alpha = 0.6f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = inputBgColor,
                                        focusedContainerColor = inputFocusedBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color.Transparent,
                                        unfocusedTextColor = textColor,
                                        focusedTextColor = textColor,
                                        cursorColor = primaryColor
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val verifyOtpInteractionSource = remember { MutableInteractionSource() }
                                val isVerifyOtpPressed by verifyOtpInteractionSource.collectIsPressedAsState()
                                val verifyOtpScale by animateFloatAsState(
                                    targetValue = if (isVerifyOtpPressed) 0.97f else 1f,
                                    label = "verifyOtpScale"
                                )

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
                                    interactionSource = verifyOtpInteractionSource,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .scale(verifyOtpScale)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            ambientColor = PrimaryBlue.copy(alpha = 0.2f),
                                            spotColor = PrimaryBlue.copy(alpha = 0.3f)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(PrimaryBlue, LightBlue)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Verify OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Didn't receive code?", color = secondaryTextColor, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Resend",
                                        color = PrimaryBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            isOtpSent = false
                                            otp = ""
                                        }
                                    )
                                }
                            }
                        } else {
                            // Phone Verified Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SuccessGreenLight)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_check_24),
                                        null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Phone Verified",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

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
                                placeholder = { Text("Password", color = TextSecondary.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = inputBgColor,
                                    focusedContainerColor = inputFocusedBgColor,
                                    focusedBorderColor = when {
                                        password.isEmpty() -> primaryColor
                                        passwordValidation.isValid -> SuccessGreen
                                        else -> Color(0xFFEF4444)
                                    },
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLeadingIconColor = primaryColor,
                                    unfocusedLeadingIconColor = secondaryTextColor,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    cursorColor = primaryColor
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) },
                                trailingIcon = {
                                    IconButton(onClick = { passVisibility = !passVisibility }) {
                                        Icon(
                                            painterResource(if (passVisibility) R.drawable.baseline_visibility_off_24 else R.drawable.baseline_visibility_24),
                                            null,
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                                shape = RoundedCornerShape(14.dp)
                            )

                            if (showPasswordRequirements) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                    Text("Password must contain:", fontSize = 13.sp, color = secondaryTextColor, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    PasswordRequirementItem("At least 8 characters", passwordValidation.hasMinLength)
                                    PasswordRequirementItem("Uppercase letter (A-Z)", passwordValidation.hasUppercase)
                                    PasswordRequirementItem("Lowercase letter (a-z)", passwordValidation.hasLowercase)
                                    PasswordRequirementItem("Number (0-9)", passwordValidation.hasDigit)
                                    PasswordRequirementItem("Special character (!@#$%)", passwordValidation.hasSpecialChar)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = inputBgColor,
                                    focusedContainerColor = inputFocusedBgColor,
                                    focusedBorderColor = if (password == confirmPassword && confirmPassword.isNotEmpty()) SuccessGreen else primaryColor,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLeadingIconColor = primaryColor,
                                    unfocusedLeadingIconColor = secondaryTextColor,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    cursorColor = primaryColor
                                ),
                                placeholder = { Text("Confirm password", color = TextSecondary.copy(alpha = 0.6f)) },
                                visualTransformation = if (!passVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(painterResource(R.drawable.baseline_lock_24), null) },
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Terms & Conditions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = terms,
                                    onCheckedChange = { terms = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryBlue,
                                        uncheckedColor = TextSecondary.copy(alpha = 0.5f),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    buildAnnotatedString {
                                        append("I agree to the ")
                                        withStyle(
                                            SpanStyle(
                                                color = PrimaryBlue,
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append("Terms & Conditions")
                                        }
                                    },
                                    style = TextStyle(fontSize = 14.sp, color = textColor),
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
                            val createAccountInteractionSource = remember { MutableInteractionSource() }
                            val isCreateAccountPressed by createAccountInteractionSource.collectIsPressedAsState()
                            val createAccountScale by animateFloatAsState(
                                targetValue = if (isCreateAccountPressed) 0.97f else 1f,
                                label = "createAccountScale"
                            )

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
                                interactionSource = createAccountInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .scale(createAccountScale)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = PrimaryBlue.copy(alpha = 0.2f),
                                        spotColor = PrimaryBlue.copy(alpha = 0.3f)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFE2E8F0)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = if (terms && !isLoading) {
                                                Brush.horizontalGradient(colors = listOf(PrimaryBlue, LightBlue))
                                            } else {
                                                Brush.horizontalGradient(colors = listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            "Create Account",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (terms) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sign In Card (Outside the main form card)
                if (isPhoneVerified) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.04f),
                                spotColor = Color.Black.copy(alpha = 0.06f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else CardWhite
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            Text(
                                "Already have an account?",
                                style = TextStyle(fontSize = 14.sp, color = secondaryTextColor)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Sign in",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = PrimaryBlue,
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