package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpitkatiyarprojects.countrypicker.CountryPickerOutlinedTextField
import com.arpitkatiyarprojects.countrypicker.enums.CountryListDisplayType
import com.example.gharbato.repository.UserRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.view.ui.theme.GharBatoTheme
import com.example.gharbato.viewmodel.UserViewModel

class OTPVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OTPVerificationBody()
        }
    }
}

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
fun OTPVerificationBody(){
    var otp by remember { mutableStateOf("") }
    val userViewModel = remember { UserViewModel(UserRepoImpl()) }
    var phoneNo by remember { mutableStateOf("") }
    var maxPhoneLength by remember { mutableStateOf(10) }
    var selectedCountry by remember { mutableStateOf("Nepal") }
    val context = LocalContext.current
    val activity = context as Activity
    var verificationIdStore by remember { mutableStateOf("") }

    Scaffold() {padding ->
        Column(
            modifier = Modifier.padding(padding )
        ) {

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

            Button(onClick = {
                userViewModel.sendOtp("+977$phoneNo", activity) {
                        success, message, verificationId ->
                    verificationIdStore = verificationId.toString()
                    if (success) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Request OTP")
            }

            OutlinedTextField(
                value = otp,
                onValueChange = {otp = it},
                placeholder = {Text("Enter OTP")}
            )

            Button(onClick = {
                userViewModel.verifyOtp(verificationIdStore, otp){
                    success, msg ->
                    if (success) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    else{
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Verify")
            }
        }
    }
}

@Composable
@Preview
fun OTPVerificationPreview(){
    OTPVerificationBody()
}