package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.viewmodel.initializer
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import com.example.gharbato.utils.ListingDraftStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListingBody(propertyId: String? = null, isEdit: Boolean = false) {

    val context = LocalContext.current
    val activity = context as Activity
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    val listingViewModel: ListingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer { ListingViewModel(PropertyRepoImpl()) }
        }
    )

    var step by rememberSaveable { mutableIntStateOf(if (isEdit) 2 else 1) }
    val formScroll = rememberLazyListState()
    var introHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(step) { formScroll.scrollToItem(0) }

    var listingState by rememberSaveable { mutableStateOf(PropertyListingState()) }
    var isLoadingProperty by rememberSaveable { mutableStateOf(true) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var isSavingDraft by remember { mutableStateOf(false) }
    var draftSaveError by remember { mutableStateOf<String?>(null) }
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
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Determine if rental terms should be shown
    val isRent = listingState.selectedPurpose == "Rent"
    val totalSteps = if (isRent) 5 else 4

    //Collect states from ViewModel
    val isUploading by listingViewModel.isUploading.collectAsState()
    val uploadProgress by listingViewModel.uploadProgress.collectAsState()
    val uploadSuccess by listingViewModel.uploadSuccess.collectAsState()
    val uploadError by listingViewModel.uploadError.collectAsState()
    LaunchedEffect(uploadSuccess, uploadError) {
        if (uploadSuccess == true) {
            withContext(Dispatchers.IO) { draftStore.clear() }
            Toast.makeText(context, "Listing submitted for approval.", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(context, PendingPropertiesActivity::class.java))
            activity.finish()
        } else if (uploadError != null) {
            showConfirmDialog = false
            Toast.makeText(context, uploadError, Toast.LENGTH_LONG).show()
            listingViewModel.resetUploadStatus()
        }
    }

    val saveDraft: () -> Unit = {
        if (!isSavingDraft && !isUploading) {
            val snapshot = listingState.copy(
                imageCategories = listingState.imageCategories.map { it.copy(images = it.images.toMutableList()) }
            )
            val savedStep = step
            draftSaveError = null
            isSavingDraft = true
            scope.launch {
                val saved = withContext(Dispatchers.IO) { draftStore.save(snapshot, savedStep) }
                isSavingDraft = false
                if (saved) {
                    showExitDialog = false
                    activity.finish()
                } else {
                    draftSaveError = "Couldn't save your draft. Check that your selected photos are still available, then try again."
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
        SaveListingProgressDialog(
            isSaving = isSavingDraft,
            saveError = draftSaveError,
            onSaveAndExit = saveDraft,
            onExitWithoutSaving = { activity.finish() },
            onKeepEditing = {
                showExitDialog = false
                draftSaveError = null
            }
        )
    }

    // Confirm Submit Dialog - Improved UI
    if (showConfirmDialog || isUploading) {
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
                                isEdit = isEdit
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

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                modifier = Modifier.testTag("listing_app_bar"),
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }, enabled = !isUploading && !isSavingDraft) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit listing")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (!isLoadingProperty) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    // Back Button
                    OutlinedButton(
                        enabled = !isUploading && !isSavingDraft,
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
                            .heightIn(min = 52.dp),
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
                        enabled = !isUploading && !isSavingDraft,
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
                            .heightIn(min = 52.dp),
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
    ) { padding ->
        if (isLoadingProperty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                    Text("Loading property details...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                    .testTag("listing_form_scroll"),
                state = formScroll,
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                if (step == 1) {
                    item(key = "listing_intro") {
                        val opacity by remember {
                            derivedStateOf {
                                if (formScroll.firstVisibleItemIndex != 0) 0f
                                else if (introHeight == 0) 1f
                                else (1f - formScroll.firstVisibleItemScrollOffset / (introHeight * 0.5f)).coerceIn(0f, 1f)
                            }
                        }
                        ListingIntro(opacity, Modifier.padding(horizontal = 16.dp).onSizeChanged { introHeight = it.height })
                    }
                }
                stickyHeader(key = "listing_progress") {
                    ListingProgressHeader(step = step, isRent = isRent)
                }
                item(key = "listing_content_$step") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                                scrollEnabled = false,
                                onStateChange = { newState ->
                                    listingState = newState
                                }
                            )
                            step == 3 -> PhotosContentScreen(
                                imageCategories = listingState.imageCategories,
                                scrollEnabled = false,
                                onCategoriesChange = { newCategories ->
                                    listingState = listingState.copy(imageCategories = newCategories)
                                }
                            )
                            step == 4 && !isRent -> AmenitiesContentScreen(
                                state = listingState,
                                scrollEnabled = false,
                                onStateChange = { newState ->
                                    listingState = newState
                                }
                            )
                            step == 4 && isRent -> RentalTermsContentScreen(
                                state = listingState,
                                scrollEnabled = false,
                                onStateChange = { newState ->
                                    listingState = newState
                                }
                            )
                            step == 5 && isRent -> AmenitiesContentScreen(
                                state = listingState,
                                scrollEnabled = false,
                                onStateChange = { newState ->
                                    listingState = newState
                                }
                            )
                        }
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
