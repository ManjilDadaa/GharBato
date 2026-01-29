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
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.getDefaultImageCategories
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.viewmodel.ListingViewModel
import com.google.firebase.database.FirebaseDatabase
import com.example.gharbato.utils.SystemBarUtils

class ListingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the theme preference
        ThemePreference.init(this)

        val propertyId = intent.getStringExtra("propertyId")
        val isEdit = intent.getBooleanExtra("isEdit", false)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                ListingBody(propertyId = propertyId, isEdit = isEdit)
            }
        }
    }
}

@Composable
fun ListingBody(propertyId: String? = null, isEdit: Boolean = false) {

    val context = LocalContext.current
    val activity = context as Activity
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    val listingViewModel = remember { ListingViewModel(PropertyRepoImpl()) }

    var step by rememberSaveable { mutableIntStateOf(if (isEdit) 2 else 1) }
    val showHeader = step == 1

    var listingState by rememberSaveable { mutableStateOf(PropertyListingState()) }
    var isLoadingProperty by remember { mutableStateOf(isEdit) }

    // Load property data if editing
    LaunchedEffect(propertyId, isEdit) {
        if (isEdit && propertyId != null) {
            FirebaseDatabase.getInstance()
                .getReference("Property")
                .child(propertyId)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.getValue(PropertyModel::class.java)?.let { property ->
                        listingState = PropertyListingState(
                            selectedPurpose = property.marketType,
                            selectedPropertyType = property.propertyType,
                            title = property.title,
                            developer = property.developer,
                            price = property.price.replace(Regex("[^0-9]"), ""),
                            location = property.location,
                            area = property.sqft.replace(Regex("[^0-9]"), ""),
                            floor = property.floor,
                            furnishing = property.furnishing,
                            bedrooms = property.bedrooms.toString(),
                            bathrooms = property.bathrooms.toString(),
                            parking = property.parking,
                            petsAllowed = property.petsAllowed,
                            description = property.description ?: "",
                            kitchen = property.kitchen,
                            totalRooms = property.totalRooms,
                            latitude = property.latitude,
                            longitude = property.longitude,
                            hasSelectedLocation = property.latitude != 27.7172 || property.longitude != 85.3240,
                            imageCategories = getDefaultImageCategories().map { category ->
                                category.copy(images = (property.images[category.id] ?: emptyList()).toMutableList())
                            },
                            utilitiesIncluded = property.utilitiesIncluded ?: "Included (electricity extra)",
                            commission = property.commission ?: "No commission",
                            advancePayment = property.advancePayment ?: "1 month rent",
                            securityDeposit = property.securityDeposit ?: "2 months rent",
                            minimumLease = property.minimumLease ?: "12 months",
                            availableFrom = property.availableFrom ?: "Immediate",
                            amenities = property.amenities
                        )
                    }
                    isLoadingProperty = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load property", Toast.LENGTH_SHORT).show()
                    isLoadingProperty = false
                }
        } else if (isEdit) {
            Toast.makeText(context, "Property ID not found", Toast.LENGTH_SHORT).show()
            isLoadingProperty = false
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Determine if rental terms should be shown
    val isRent = listingState.selectedPurpose == "Rent"
    val totalSteps = if (isRent) 5 else 4

    //Collect states from ViewModel
    val isUploading by listingViewModel.isUploading.collectAsState()
    val uploadProgress by listingViewModel.uploadProgress.collectAsState()
    val uploadSuccess by listingViewModel.uploadSuccess.collectAsState()

    // Exit Dialog - Improved UI
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = if (isEdit) "Cancel Editing?" else "Exit Listing?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                )
            },
            text = {
                Text(
                    text = if (isEdit) "Are you sure you want to cancel? Any unsaved changes will be lost."
                    else "Are you sure you want to go back? Your progress won't be saved.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        activity.finish()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = if (isEdit) "Yes, Cancel" else "Yes, Exit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isDarkMode) MaterialTheme.colorScheme.outline else Gray.copy(0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue Editing",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
            tonalElevation = 0.dp
        )
    }

    // Confirm Submit Dialog - Improved UI
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showConfirmDialog = false },
            title = {
                Text(
                    text = if (isUploading) "Uploading..." else "Submit Listing?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = Blue
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uploadProgress,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Review your listing:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            ReviewItem("Purpose", listingState.selectedPurpose, isDarkMode)
                            ReviewItem("Type", listingState.selectedPropertyType, isDarkMode)
                            ReviewItem("Title", listingState.title, isDarkMode)
                            ReviewItem("Price", "Rs ${listingState.price}", isDarkMode)
                            ReviewItem("Location", listingState.location, isDarkMode)

                            val totalImages = listingState.imageCategories.sumOf { it.images.size }
                            ReviewItem("Images", "$totalImages photos", isDarkMode)
                        }
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
                                propertyId = propertyId,
                                isEdit = isEdit,
                                onSuccess = {
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                    Toast.makeText(context, if (isEdit) "Property updated successfully" else "Listing Added successfully, Admin Approval Required", Toast.LENGTH_LONG).show()
                                    val intent = Intent(context, PendingPropertiesActivity::class.java)
                                    context.startActivity(intent)
                                },
                                onError = { error ->
                                    showConfirmDialog = false
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Yes, Submit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {
                if (!isUploading) {
                    OutlinedButton(
                        onClick = { showConfirmDialog = false },
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isDarkMode) MaterialTheme.colorScheme.outline else Gray.copy(0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
            tonalElevation = 0.dp
        )
    }

    // Success Dialog - Improved UI
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Success!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF10B981)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_check_24),
                            contentDescription = "Success icon",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isEdit) "Your property has been updated successfully!"
                        else "Your property has been listed successfully!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A),
                        modifier = Modifier.padding(horizontal = 8.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Go to Dashboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
            tonalElevation = 0.dp
        )
    }

    Scaffold(
        containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFFAFAFA)
    ) { padding ->
        if (isLoadingProperty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Blue,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Loading property details...",
                        fontSize = 15.sp,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Gray
                    )
                }
            }
        } else {
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
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + expandVertically(
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    ) + shrinkVertically(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(color = Blue, shape = RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.home),
                                    contentDescription = "Property icon",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "List Your Property",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color(0xFF1A1A1A)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Reach thousands of potential buyers and renters",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Gray
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (showHeader) 16.dp else 32.dp))

                // Progress Indicator - Improved UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalSteps) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val circleColor by animateColorAsState(
                                targetValue = if (step >= i) Blue else if (isDarkMode) Color(0xFF424242) else Gray.copy(0.25f),
                                animationSpec = tween(durationMillis = 300),
                                label = "stepColor"
                            )

                            Box(
                                modifier = Modifier
                                    .background(color = circleColor, shape = CircleShape)
                                    .size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$i",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = when {
                                    i == 1 -> "Purpose"
                                    i == 2 -> "Details"
                                    i == 3 -> "Photos"
                                    i == 4 && !isRent -> "Amenities"
                                    i == 4 && isRent -> "Terms"
                                    i == 5 -> "Amenities"
                                    else -> ""
                                },
                                fontSize = 13.sp,
                                fontWeight = if (step == i) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (step >= i) {
                                    if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                                } else {
                                    if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Gray
                                },
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }

                        if (i < totalSteps) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 0.dp)
                                    .weight(0.5f)
                                    .height(3.dp)
                                    .background(
                                        color = if (step > i) Blue else if (isDarkMode) Color(0xFF424242) else Gray.copy(0.25f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content based on step and purpose
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
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
                        step == 4 && !isRent -> AmenitiesContentScreen(
                            state = listingState,
                            onStateChange = { newState ->
                                listingState = newState
                            }
                        )
                        step == 4 && isRent -> RentalTermsContentScreen(
                            state = listingState,
                            onStateChange = { newState ->
                                listingState = newState
                            }
                        )
                        step == 5 && isRent -> AmenitiesContentScreen(
                            state = listingState,
                            onStateChange = { newState ->
                                listingState = newState
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Buttons - Improved UI
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Back Button
                    OutlinedButton(
                        onClick = {
                            val newStep = step - 1
                            val minStep = if (isEdit) 2 else 1
                            if (newStep < minStep) {
                                showExitDialog = true
                            } else {
                                step -= 1
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isDarkMode) MaterialTheme.colorScheme.outline else Gray.copy(0.4f)
                            )
                        )
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                        )
                    }

                    // Next/Submit Button
                    Button(
                        onClick = {
                            val validationResult = listingViewModel.validateStep(step, listingState)

                            if (validationResult.isValid) {
                                if (step < totalSteps) {
                                    step += 1
                                } else {
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
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = if (step == totalSteps) "Submit" else if (step == 1) "Continue" else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Helper composable for review items in confirm dialog
@Composable
private fun ReviewItem(label: String, value: String, isDarkMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF666666),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A),
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}