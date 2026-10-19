package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.gharbato.utils.ListingDraftStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
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
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import com.example.gharbato.ui.theme.LocalGharBatoActionColors
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingBody(propertyId: String? = null, isEdit: Boolean = false) {

    val context = LocalContext.current
    val activity = context as Activity
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    val listingViewModel = remember { ListingViewModel(PropertyRepoImpl()) }

    var step by rememberSaveable { mutableIntStateOf(if (isEdit) 2 else 1) }
    val showHeader = step == 1

    var listingState by rememberSaveable { mutableStateOf(PropertyListingState()) }
    var isLoadingProperty by rememberSaveable { mutableStateOf(true) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var isSavingDraft by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    val draftStore = remember(userId, propertyId) { ListingDraftStore(context, userId, propertyId) }

    // Saved instance state takes precedence on rotation; otherwise resume the local draft.
    LaunchedEffect(propertyId, isEdit) {
        if (initialized && !isLoadingProperty) return@LaunchedEffect
        initialized = true
        val draft = withContext(Dispatchers.IO) { draftStore.load() }
        if (draft != null) {
            listingState = draft.state
            step = draft.step.coerceAtLeast(if (isEdit) 2 else 1)
            isLoadingProperty = false
            snackbar.showSnackbar("Draft restored. Continue where you left off.")
            return@LaunchedEffect
        }
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
        } else {
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

    val saveDraft: (Boolean) -> Unit = { exitAfterSaving ->
        if (!isSavingDraft && !isUploading) {
            val snapshot = listingState.copy(
                imageCategories = listingState.imageCategories.map { it.copy(images = it.images.toMutableList()) }
            )
            val savedStep = step
            isSavingDraft = true
            scope.launch {
                val saved = withContext(Dispatchers.IO) { draftStore.save(snapshot, savedStep) }
                isSavingDraft = false
                if (saved) {
                    showExitDialog = false
                    if (exitAfterSaving) activity.finish() else snackbar.showSnackbar("Draft saved. You can return to it later.")
                } else {
                    snackbar.showSnackbar("Couldn't save the draft. Check that your photos are still available and try again.")
                }
            }
        }
    }
    BackHandler {
        if (!isUploading && !isSavingDraft) {
            if (step > if (isEdit) 2 else 1) step-- else showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingDraft) showExitDialog = false },
            title = { Text("Save your progress?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save a draft to finish your listing later, including your selected photos.")
                    TextButton(onClick = { activity.finish() }, enabled = !isSavingDraft) { Text("Exit without saving") }
                }
            },
            confirmButton = {
                Button(onClick = { saveDraft(true) }, enabled = !isSavingDraft, colors = GharBatoButtonDefaults.primaryColors()) {
                    Text(if (isSavingDraft) "Saving..." else "Save and exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }, enabled = !isSavingDraft) { Text("Keep editing") }
            }
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
                    color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uploadProgress,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Review your listing:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
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
                                    scope.launch(Dispatchers.IO) { draftStore.clear() }
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
                        colors = GharBatoButtonDefaults.primaryColors(),
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
                        colors = GharBatoButtonDefaults.outlinedColors(),
                        border = GharBatoButtonDefaults.outlinedBorder(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
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
                            .size(56.dp)
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
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isEdit) "Your property has been updated successfully!"
                        else "Your property has been listed successfully!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
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
                    colors = GharBatoButtonDefaults.primaryColors(),
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
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit property" else "List your property", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }, enabled = !isUploading && !isSavingDraft) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit listing")
                    }
                },
                actions = {
                    TextButton(onClick = { saveDraft(false) }, enabled = !isLoadingProperty && !isUploading && !isSavingDraft) {
                        Text(if (isSavingDraft) "Saving..." else "Save draft")
                    }
                }
            )
        }
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
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Loading property details...",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .size(56.dp)
                                    .background(color = LocalGharBatoActionColors.current.container, shape = RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.home),
                                    contentDescription = "Property icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = LocalGharBatoActionColors.current.onContainer
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
                                    fontSize = 24.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Reach thousands of potential buyers and renters",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (showHeader) 16.dp else 32.dp))

                // Progress Indicator - Improved UI
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    val stepCircleSize = 36.dp
                    val stepColumnWidth = maxWidth / totalSteps.toFloat()
                    val stepperPrimaryColor = LocalGharBatoActionColors.current.container
                    val inactiveLine = MaterialTheme.colorScheme.outlineVariant

                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(stepCircleSize)
                    ) {
                        val slotWidth = size.width / totalSteps.toFloat()
                        val circleRadius = stepCircleSize.toPx() / 2f
                        val centerY = circleRadius
                        val strokeWidth = 3.dp.toPx()
                        for (index in 0 until totalSteps - 1) {
                            val startX = slotWidth * (index + 0.5f) + circleRadius
                            val endX = slotWidth * (index + 1.5f) - circleRadius
                            drawLine(
                                color = inactiveLine,
                                start = Offset(startX, centerY),
                                end = Offset(endX, centerY),
                                strokeWidth = strokeWidth
                            )
                            if (index < step - 1) {
                                drawLine(
                                    color = stepperPrimaryColor,
                                    start = Offset(startX, centerY),
                                    end = Offset(endX, centerY),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        for (i in 1..totalSteps) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(stepColumnWidth)
                            ) {
                                val isComplete = step >= i
                                val circleColor by animateColorAsState(
                                    targetValue = if (isComplete) LocalGharBatoActionColors.current.container else MaterialTheme.colorScheme.outlineVariant,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "stepColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .background(color = circleColor, shape = CircleShape)
                                        .size(stepCircleSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$i",
                                        color = if (isComplete) LocalGharBatoActionColors.current.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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
                                    fontSize = 12.sp,
                                    fontWeight = if (step == i) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (step >= i) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
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
                        colors = GharBatoButtonDefaults.outlinedColors(),
                        border = GharBatoButtonDefaults.outlinedBorder()
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
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
                        colors = GharBatoButtonDefaults.primaryColors(),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}
