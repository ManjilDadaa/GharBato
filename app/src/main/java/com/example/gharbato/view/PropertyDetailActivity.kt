package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.ReportStatus
import com.example.gharbato.model.ReportedProperty
import com.example.gharbato.repository.MessageRepositoryImpl
import com.example.gharbato.repository.NearbyPlacesRepositoryImpl
import com.example.gharbato.repository.ReportPropertyRepoImpl
import com.example.gharbato.ui.theme.GharBatoShapes
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.ui.view.FullMapActivity
import com.example.gharbato.util.PropertyViewTracker
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.utils.formatNprAmount
import com.example.gharbato.utils.formatPropertyPrice
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PropertyDetailActivity : ComponentActivity() {
    private val viewModel: PropertyViewModel by viewModels { PropertyViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        val propertyId = intent.getIntExtra("propertyId", -1)
        if (propertyId != -1) {
            viewModel.getPropertyById(propertyId)
            PropertyViewTracker.trackPropertyViewById(propertyId)
        }
        setContent {
            val dark by ThemePreference.isDarkModeState.collectAsState()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, dark)
            GharBatoTheme(darkTheme = dark) {
                val property = state.selectedProperty
                if (property != null) {
                    LaunchedEffect(property.id) { viewModel.loadSimilarProperties(property) }
                    PropertyDetailScreen(
                        property = property,
                        similarProperties = state.similarProperties,
                        isLoadingSimilar = state.isLoadingSimilar,
                        onBack = { viewModel.clearSimilarProperties(); finish() },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        onSimilarPropertyClick = { similar ->
                            startActivity(Intent(this, PropertyDetailActivity::class.java).putExtra("propertyId", similar.id))
                        }
                    )
                } else {
                    PropertyDetailLoading(state.isLoading, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyDetailLoading(loading: Boolean, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Property", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(16.dp))
                Text("Opening property...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Property not found", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("This listing may no longer be available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onBack) { Text("Back to homes") }
            }
        }
    }
}

@Composable
fun PropertyDetailScreen(
    property: PropertyModel,
    similarProperties: List<PropertyModel>,
    isLoadingSimilar: Boolean,
    onBack: () -> Unit,
    onFavoriteToggle: (PropertyModel) -> Unit,
    onSimilarPropertyClick: (PropertyModel) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showReport by rememberSaveable(property.id) { mutableStateOf(false) }
    var showOffer by rememberSaveable(property.id) { mutableStateOf(false) }
    var ownerName by remember(property.id) { mutableStateOf(property.ownerName) }
    var ownerImage by remember(property.id) { mutableStateOf(property.ownerImageUrl) }
    var nearby by remember(property.id) { mutableStateOf(emptyList<NearbyPlace>()) }
    var loadingNearby by remember(property.id) { mutableStateOf(false) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val canContact = property.ownerId.isNotBlank() && property.ownerId != uid
    val displayProperty = property.copy(ownerName = ownerName, ownerImageUrl = ownerImage)

    LaunchedEffect(property.ownerId) {
        if (property.ownerId.isNotBlank()) {
            try {
                val owner = FirebaseDatabase.getInstance().getReference("Users").child(property.ownerId).get().await()
                owner.child("fullName").getValue(String::class.java)?.takeIf(String::isNotBlank)?.let { ownerName = it }
                owner.child("profileImageUrl").getValue(String::class.java)?.takeIf(String::isNotBlank)?.let { ownerImage = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The stored listing remains usable when the current profile cannot be fetched.
            }
        }
    }
    LaunchedEffect(property.latitude, property.longitude) {
        if (property.hasValidCoordinates) {
            loadingNearby = true
            try {
                nearby = NearbyPlacesRepositoryImpl().getNearbyPlaces(property.latLng).values.flatten()
                    .filterNot { it.id.startsWith("fallback_") }.distinctBy { it.id }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                nearby = emptyList()
            } finally {
                loadingNearby = false
            }
        }
    }

    val requireSignIn: (() -> Unit) -> Unit = { action ->
        if (uid.isNotBlank()) action() else scope.launch {
            if (snackbar.showSnackbar("Sign in to contact this owner.", actionLabel = "Sign in") == SnackbarResult.ActionPerformed) {
                context.startActivity(Intent(context, LoginActivity::class.java))
            }
        }
    }
    PropertyDetailsContent(
        property = displayProperty,
        similarProperties = similarProperties,
        isLoadingSimilar = isLoadingSimilar,
        nearbyPlaces = nearby,
        isLoadingNearby = loadingNearby,
        onBack = onBack,
        onFavoriteClick = { onFavoriteToggle(property) },
        onShareClick = { shareProperty(context, property) },
        onMessageClick = { requireSignIn { openPropertyConversation(activity, displayProperty) } },
        onCallClick = { requireSignIn {
            MessageRepositoryImpl().initiateCall(activity, property.ownerId, displayProperty.ownerName.ifBlank { "Property owner" }, false)
        } },
        onOfferClick = { requireSignIn { showOffer = true } },
        onReportClick = {
            if (uid.isNotBlank()) showReport = true else scope.launch { snackbar.showSnackbar("Sign in to report a listing.") }
        },
        onMapClick = {
            context.startActivity(Intent(context, FullMapActivity::class.java).apply {
                putExtra("latitude", property.latitude)
                putExtra("longitude", property.longitude)
                putExtra("propertyName", property.title.ifBlank { property.propertyType })
            })
        },
        onSimilarPropertyClick = onSimilarPropertyClick,
        canContact = canContact,
        isOwnListing = property.ownerId.isNotBlank() && property.ownerId == uid,
        snackbarHostState = snackbar
    )
    if (showReport) {
        ReportListingDialog(
            onDismiss = { showReport = false },
            onSubmit = { reason, details ->
                showReport = false
                val report = ReportedProperty(
                    propertyId = property.id, propertyTitle = property.title.ifBlank { property.propertyType },
                    propertyImage = property.imageUrl, ownerId = property.ownerId,
                    ownerName = displayProperty.ownerName, reportedBy = uid, reportReason = reason,
                    reportDetails = details, reportedAt = System.currentTimeMillis(), status = ReportStatus.PENDING
                )
                scope.launch {
                    val result = ReportPropertyRepoImpl().reportProperty(report)
                    snackbar.showSnackbar(if (result.isSuccess) "Report submitted. Thank you for letting us know." else "Couldn't submit the report. Please try again.")
                }
            }
        )
    }
    if (showOffer) {
        PropertyOfferSheet(property, onDismiss = { showOffer = false }) { amount ->
            showOffer = false
            Toast.makeText(context, "Sending offer...", Toast.LENGTH_SHORT).show()
            MessageRepositoryImpl().sendQuickMessageWithPropertyAndNavigate(
                context = context, activity = activity, otherUserId = property.ownerId,
                otherUserName = displayProperty.ownerName.ifBlank { "Property owner" }, otherUserImage = displayProperty.ownerImageUrl,
                message = "Hi, I'm interested in ${property.title.ifBlank { property.propertyType }}. I'd like to offer $amount.",
                property = displayProperty
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyOfferSheet(property: PropertyModel, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val numericAmount = amount.toBigDecimalOrNull()
    val valid = numericAmount != null && numericAmount.signum() > 0
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        shape = GharBatoShapes.large, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Make an offer", style = MaterialTheme.typography.headlineSmall)
            Text(property.title.ifBlank { property.propertyType }, style = MaterialTheme.typography.bodyLarge)
            Text("Asking price: ${formatPropertyPrice(property.price, property.marketType)}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(10) },
                label = { Text("Your offer (NPR)") }, prefix = { Text("Rs ") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = GharBatoShapes.control,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = attempted && !valid,
                supportingText = {
                    if (attempted && !valid) Text("Enter an amount greater than zero.") else Text(when (property.marketType.lowercase()) {
                        "rent" -> "Monthly rent in Nepalese rupees"
                        "book" -> "Nightly price in Nepalese rupees"
                        else -> "Total amount in Nepalese rupees"
                    })
                }
            )
            Button(onClick = { attempted = true; if (valid) onSubmit(formatNprAmount(numericAmount!!)) },
                colors = GharBatoButtonDefaults.primaryColors(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = GharBatoShapes.control) {
                Text("Send offer")
            }
        }
    }
}

private fun openPropertyConversation(activity: Activity, property: PropertyModel) {
    if (property.ownerId.isBlank()) return
    activity.startActivity(MessageDetailsActivity.newIntent(activity, property.ownerId,
        property.ownerName.ifBlank { "Property owner" }, property.ownerImageUrl))
}

private fun shareProperty(context: Context, property: PropertyModel) {
    val title = property.title.ifBlank { property.propertyType }
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n${formatPropertyPrice(property.price, property.marketType)}\n${property.location}\n\nFind this property on GharBato.")
    }
    context.startActivity(Intent.createChooser(share, "Share property"))
}

/** Compatibility for screens embedding the contact actions; the visual component is shared. */
@Composable
fun BoxScope.BottomActionButtons(
    property: PropertyModel,
    surfaceColor: Color,
    outlineVariantColor: Color,
    successColor: Color,
    primaryColor: Color
) {
    val context = LocalContext.current
    Box(Modifier.align(Alignment.BottomCenter)) {
        PropertyContactBar(
            onMessageClick = { openPropertyConversation(context as Activity, property) },
            onCallClick = { MessageRepositoryImpl().initiateCall(context as Activity, property.ownerId, property.ownerName.ifBlank { "Property owner" }, false) },
            enabled = property.ownerId.isNotBlank()
        )
    }
}
