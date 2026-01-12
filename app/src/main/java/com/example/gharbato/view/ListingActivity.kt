package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.data.repository.PropertyRepoImpl
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.ListingViewModel

class ListingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListingBody()
        }
    }
}

@Composable
fun ListingBody() {

    val context = LocalContext.current
    val activity = context as Activity
    val listingViewModel = remember { ListingViewModel(PropertyRepoImpl()) }

    var step by rememberSaveable { mutableIntStateOf(1) }
    val showHeader = step == 1

    var listingState by rememberSaveable { mutableStateOf(PropertyListingState()) }

    var showExitDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Determine if rental terms should be shown
    val isRentOrBook = listingState.selectedPurpose == "Rent" || listingState.selectedPurpose == "Book"
    val totalSteps = if (isRentOrBook) 5 else 4 // Sell: 1,2,3,4(Amenities) | Rent/Book: 1,2,3,4(Terms),5(Amenities)

    //Collect states from ViewModel
    val isUploading by listingViewModel.isUploading.collectAsState()
    val uploadProgress by listingViewModel.uploadProgress.collectAsState()
    val uploadSuccess by listingViewModel.uploadSuccess.collectAsState()

    // Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Exit Listing?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to go back? Your progress won't be saved.",
                    fontSize = 15.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    )
                ) {
                    Text("Yes, Exit")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gray.copy(0.7f)
                    )
                ) {
                    Text("Stay")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirm Submit Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showConfirmDialog = false },
            title = {
                Text(
                    if (isUploading) "Uploading..." else "Submit Listing?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            uploadProgress,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Review your listing:", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Purpose: ${listingState.selectedPurpose}", fontSize = 14.sp)
                        Text("• Type: ${listingState.selectedPropertyType}", fontSize = 14.sp)
                        Text("• Title: ${listingState.title}", fontSize = 14.sp)
                        Text("• Price: Rs ${listingState.price}", fontSize = 14.sp)
                        Text("• Location: ${listingState.location}", fontSize = 14.sp)

                        val totalImages = listingState.imageCategories.sumOf { it.images.size }
                        Text("• Images: $totalImages photos", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                if (!isUploading) {
                    Button(
                        onClick = {
                            listingViewModel.submitListing(
                                context = context,
                                state = listingState,
                                onSuccess = {
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                    Toast.makeText(context, "Listing Added successfully, Admin Approval Required ", Toast.LENGTH_LONG).show()
                                },
                                onError = { error ->
                                    showConfirmDialog = false
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text("Yes, Submit")
                    }
                }
            },
            dismissButton = {
                if (!isUploading) {
                    Button(
                        onClick = { showConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Gray.copy(0.7f))
                    ) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "Success!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your property has been listed successfully!",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        listingViewModel.resetUploadStatus()
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text("Go to Dashboard")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with animation
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 600)
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 600)
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(color = Blue, shape = RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.home),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "List Your Property",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            "Reach thousands of potential buyers and renters",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Progress Indicator - Dynamic based on purpose
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    Column {
                        val circleColor by animateColorAsState(
                            targetValue = if (step >= i) Blue else Gray.copy(0.3f),
                            label = "stepColor"
                        )
                        Box(
                            modifier = Modifier
                                .background(color = circleColor, shape = CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$i", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when {
                                i == 1 -> "Purpose"
                                i == 2 -> "Details"
                                i == 3 -> "Photos"
                                i == 4 && !isRentOrBook -> "Amenities" // For Sell
                                i == 4 && isRentOrBook -> "Terms" // For Rent/Book
                                i == 5 -> "Amenities"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }

                    if (i < totalSteps) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 7.dp)
                                .weight(1f)
                                .height(2.dp)
                                .background(if (step > i) Blue else Gray.copy(0.3f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content based on step and purpose
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                when {
                    step == 1 -> PurposeContentScreen(
                        selectedPurpose = listingState.selectedPurpose,
                        selectedPropertyType = listingState.selectedPropertyType,
                        onPropertyTypeChange = { newType ->
                            listingState = listingState.copy(selectedPropertyType = newType)
                        },
                        onPurposeChange = { newPurpose ->
                            listingState = listingState.copy(selectedPurpose = newPurpose)
                        }
                    )
                    step == 2 -> DetailsContentScreen(
                        state = listingState,
                        onStateChange = { newState ->
                            listingState = newState
                        }
                    )
                    step == 3 -> PhotosContentScreen(
                        imageCategories = listingState.imageCategories,
                        onCategoriesChange = { newCategories ->
                            listingState = listingState.copy(imageCategories = newCategories)
                        }
                    )
                    step == 4 && !isRentOrBook -> AmenitiesContentScreen(
                        state = listingState,
                        onStateChange = { newState ->
                            listingState = newState
                        }
                    )
                    step == 4 && isRentOrBook -> RentalTermsContentScreen(
                        state = listingState,
                        onStateChange = { newState ->
                            listingState = newState
                        }
                    )
                    step == 5 && isRentOrBook -> AmenitiesContentScreen(
                        state = listingState,
                        onStateChange = { newState ->
                            listingState = newState
                        }
                    )
                }
            }

            // Navigation Buttons
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                // Back Button
                Button(
                    onClick = {
                        val newStep = step - 1
                        if (newStep <= 0) {
                            showExitDialog = true
                        } else {
                            step -= 1
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gray.copy(0.7f)
                    )
                ) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.width(7.dp))

                // Next/Submit Button
                Button(
                    onClick = {
                        val validationResult = listingViewModel.validateStep(step, listingState)

                        if (validationResult.isValid) {
                            if (step < totalSteps) {
                                // Move to next step
                                step += 1
                            } else {
                                // Submit at final step
                                showConfirmDialog = true
                            }
                        } else {
                            Toast.makeText(
                                context,
                                validationResult.errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text(
                        if (step == totalSteps) "Submit" else if (step == 1) "Continue" else "Next"
                    )
                }
            }
        }
    }
}