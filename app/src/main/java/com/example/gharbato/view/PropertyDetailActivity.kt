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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.data.model.PlaceType
import com.example.gharbato.model.*
import com.example.gharbato.repository.MessageRepositoryImpl
import com.example.gharbato.repository.NearbyPlacesRepositoryImpl
import com.example.gharbato.repository.ReportPropertyRepoImpl
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.ui.view.FullMapActivity
import com.example.gharbato.util.PropertyViewTracker
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.viewmodel.MessageViewModel
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.example.gharbato.viewmodel.ReportViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.*
import com.example.gharbato.R
import kotlin.collections.emptyMap

private fun getCurrentUserId(): String {
    return FirebaseAuth.getInstance().currentUser?.uid ?: ""
}

private fun navigateToMessageWithUserFetch(
    context: Context,
    activity: Activity,
    otherUserId: String,
    fallbackName: String,
    fallbackImage: String = ""
) {
    // Fetch actual user data from Firebase before navigating
    // Note: Uses "Users" with capital U to match MessageRepository.fetchUsersByIds
    FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Use UserModel field names: fullName, profileImageUrl
                val actualFullName = snapshot.child("fullName").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() } ?: fallbackName
                val actualProfileImage = snapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: fallbackImage

                val intent = MessageDetailsActivity.newIntent(
                    activity = activity,
                    otherUserId = otherUserId,
                    otherUserName = actualFullName,
                    otherUserImage = actualProfileImage
                )
                activity.startActivity(intent)
            }

            override fun onCancelled(error: DatabaseError) {
                // Fall back to passed name if fetch fails
                val intent = MessageDetailsActivity.newIntent(
                    activity = activity,
                    otherUserId = otherUserId,
                    otherUserName = fallbackName,
                    otherUserImage = fallbackImage
                )
                activity.startActivity(intent)
            }
        })
}

fun getAmenityIconForPropertyDetail(amenity: String): ImageVector {
    return when (amenity.lowercase()) {
        "wifi" -> Icons.Default.Wifi
        "parking" -> Icons.Default.DirectionsCar
        "swimming pool" -> Icons.Default.Pool
        "gym" -> Icons.Default.FitnessCenter
        "garden" -> Icons.Default.Yard
        "security" -> Icons.Default.Security
        "elevator" -> Icons.Default.Elevator
        "air conditioning" -> Icons.Default.AcUnit
        "heating" -> Icons.Default.Thermostat
        "laundry" -> Icons.Default.LocalLaundryService
        "balcony" -> Icons.Default.Balcony
        "terrace" -> Icons.Default.Deck
        "garage" -> Icons.Default.Garage
        "concierge" -> Icons.Default.SupportAgent
        "pet friendly" -> Icons.Default.Pets
        "furnished" -> Icons.Default.Weekend
        "unfurnished" -> Icons.Default.Weekend
        "partially furnished" -> Icons.Default.Weekend
        else -> Icons.Default.CheckCircle
    }
}

class PropertyDetailActivity : ComponentActivity() {

    private val viewModel: PropertyViewModel by viewModels {
        PropertyViewModelFactory(this@PropertyDetailActivity)
    }

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
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            uiState.selectedProperty?.let { property ->
                LaunchedEffect(property.id) {
                    viewModel.loadSimilarProperties(property)
                }

                GharBatoTheme(darkTheme = isDarkMode) {
                    PropertyDetailScreen(
                        property = property,
                        similarProperties = uiState.similarProperties,
                        isLoadingSimilar = uiState.isLoadingSimilar,
                        onBack = {
                            viewModel.clearSimilarProperties()
                            finish()
                        },
                        onFavoriteToggle = { prop ->
                            viewModel.toggleFavorite(prop)
                        },
                        onSimilarPropertyClick = { similarProperty ->
                            val intent = Intent(this@PropertyDetailActivity, PropertyDetailActivity::class.java).apply {
                                putExtra("propertyId", similarProperty.id)
                            }
                            startActivity(intent)
                        },
                        isDarkMode = isDarkMode
                    )
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text("Property not found")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    property: PropertyModel,
    similarProperties: List<PropertyModel>,
    isLoadingSimilar: Boolean,
    onBack: () -> Unit,
    onFavoriteToggle: (PropertyModel) -> Unit,
    onSimilarPropertyClick: (PropertyModel) -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    val reportViewModel = remember { ReportViewModel(ReportPropertyRepoImpl()) }
    val reportUiState by reportViewModel.uiState.collectAsStateWithLifecycle()

    if (showReportDialog) {
        ReportListingDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                val report = ReportedProperty(
                    reportId = "",
                    propertyId = property.id,
                    propertyTitle = property.developer,
                    propertyImage = property.images.values.flatten().firstOrNull() ?: "",
                    ownerId = property.ownerId,
                    ownerName = property.ownerName.ifBlank { property.developer },
                    reportedByName = "",
                    reportedBy = getCurrentUserId(),
                    reportReason = reason,
                    reportDetails = details,
                    reportedAt = System.currentTimeMillis(),
                    status = ReportStatus.PENDING
                )
                reportViewModel.submitReport(report)
                showReportDialog = false
            }
        )
    }

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val onBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val onSurfaceColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val onSurfaceVariantColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val surfaceVariantColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
    val outlineVariantColor = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E0E0)
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val successColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)

    LaunchedEffect(reportUiState.successMessage) {
        reportUiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            reportViewModel.clearMessages()
        }
    }

    LaunchedEffect(reportUiState.error) {
        reportUiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            reportViewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Image Gallery Section
                item {
                    PropertyImageSection(
                        property = property,
                        isFavorite = property.isFavorite,
                        onFavoriteClick = { onFavoriteToggle(property) },
                        onBackClick = onBack,
                        surfaceColor = surfaceColor,
                        onSurfaceColor = onSurfaceColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Status Chips
                item {
                    StatusChipsRow(property = property, isDarkMode = isDarkMode)
                }

                // Price Section
                item {
                    PriceSection(
                        property = property,
                        surfaceColor = surfaceColor,
                        surfaceVariantColor = surfaceVariantColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineVariantColor = outlineVariantColor,
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                }

                // Property Details
                item {
                    PropertyDetailsSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                }

                // Building Info
                item {
                    BuildingInfoSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Description Section
                if (!property.description.isNullOrBlank()) {
                    item {
                        DescriptionSection(
                            property = property,
                            surfaceVariantColor = surfaceVariantColor,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor,
                            outlineVariantColor = outlineVariantColor
                        )
                    }
                }

                // Map Preview
                item {
                    MapPreviewSection(
                        property = property,
                        onClick = {
                            val intent = Intent(context, FullMapActivity::class.java).apply {
                                putExtra("latitude", property.latLng.latitude)
                                putExtra("longitude", property.latLng.longitude)
                                putExtra("propertyName", property.developer)
                            }
                            context.startActivity(intent)
                        },
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Contact Owner Section
                item {
                    ContactOwnerSection(
                        property = property,
                        surfaceColor = surfaceColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineVariantColor = outlineVariantColor,
                        isDarkMode = isDarkMode,
                        successColor = successColor
                    )
                }

                // Notes Section
                item {
                    NotesSection(
                        surfaceVariantColor = surfaceVariantColor,
                        onSurfaceColor = onSurfaceColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Property Details Info
                item {
                    PropertyDetailsInfoSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                }

                // Rental Terms
                item {
                    RentalTermsSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                }

                // Amenities
                item {
                    AmenitiesSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceColor = onSurfaceColor,
                        successColor = successColor
                    )
                }

                // Similar Properties Section
                item {
                    SimilarPropertiesSection(
                        similarProperties = similarProperties,
                        isLoading = isLoadingSimilar,
                        onPropertyClick = onSimilarPropertyClick,
                        surfaceColor = surfaceColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineVariantColor = outlineVariantColor,
                        successColor = successColor,
                        primaryColor = primaryColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Report Section
                item {
                    ReportSection(
                        onReportClick = { showReportDialog = true },
                        backgroundColor = if (isDarkMode) Color(0xFF2D1B1B) else Color(0xFFFCE4EC),
                        onBackgroundColor = onBackgroundColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            BottomActionButtons(
                property = property,
                surfaceColor = surfaceColor,
                outlineVariantColor = outlineVariantColor,
                successColor = successColor,
                primaryColor = primaryColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyImageSection(
    property: PropertyModel,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onBackClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    isDarkMode: Boolean
) {
    val context = LocalContext.current

    // Get all images from all categories
    val allImages = property.images.values.flatten()

    // If no images, show placeholder
    val imagesToShow = if (allImages.isEmpty()) {
        listOf("https://via.placeholder.com/600x400?text=No+Image")
    } else {
        allImages
    }

    // Pager state for swiping
    val pagerState = rememberPagerState(pageCount = { imagesToShow.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Image(
                painter = rememberAsyncImagePainter(imagesToShow[page]),
                contentDescription = "Property Image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top Bar with Back, Favorite, and Share buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(surfaceColor.copy(alpha = if (isDarkMode) 0.8f else 0.9f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = onSurfaceColor
                )
            }

            Row {
                // Favorite Button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(surfaceColor.copy(alpha = if (isDarkMode) 0.8f else 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else onSurfaceColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        shareProperty(context, property)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(surfaceColor.copy(alpha = if (isDarkMode) 0.8f else 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = onSurfaceColor
                    )
                }
            }
        }

        // Image counter indicator (bottom-right)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${imagesToShow.size}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Dot indicators at bottom center
        if (imagesToShow.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(imagesToShow.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .background(
                                color = if (index == pagerState.currentPage)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

// Share Property function
private fun shareProperty(context: Context, property: PropertyModel) {
    // Create a shareable message with property details
    val shareText = buildString {
        append("🏠 ${property.developer}\n\n")
        append("💰 Price: ${property.price}\n")
        append("📍 Location: ${property.location}\n")
        append("🛏️ Bedrooms: ${property.bedrooms}\n")
        append("🛁 Bathrooms: ${property.bathrooms}\n")
        append("📐 Area: ${property.sqft}\n\n")

        append("View property: https://gharbato.app/property/${property.id}\n\n")

        append("Check out this amazing property on Gharbato!")
    }

    // Create share intent
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this property: ${property.developer}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    // Show system share sheet
    context.startActivity(
        Intent.createChooser(shareIntent, "Share Property via")
    )
}

@Composable
fun StatusChipsRow(property: PropertyModel, isDarkMode: Boolean) {
    val featuredBg = if (isDarkMode) Color(0xFF332900) else Color(0xFFFFECB3)
    val featuredText = if (isDarkMode) Color(0xFFFFD54F) else Color(0xFFFF6F00)
    val verifiedBg = if (isDarkMode) Color(0xFF1B3221) else Color(0xFFE8F5E9)
    val verifiedText = if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
    val ownerBg = if (isDarkMode) Color(0xFF1A237E) else Color(0xFFE3F2FD)
    val ownerText = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF2196F3)
    val soldBg = if (isDarkMode) Color(0xFF2D1B1B) else Color(0xFFFFEBEE)
    val soldText = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val onHoldBg = if (isDarkMode) Color(0xFF332900) else Color(0xFFFFF3E0)
    val onHoldText = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFFF6F00)

    LazyRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show property status first if not AVAILABLE
        if (property.propertyStatus == "SOLD") {
            item {
                StatusChip(
                    text = "Sold",
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = soldBg,
                    textColor = soldText
                )
            }
        } else if (property.propertyStatus == "ON_HOLD") {
            item {
                StatusChip(
                    text = "On Hold",
                    icon = Icons.Default.Schedule,
                    backgroundColor = onHoldBg,
                    textColor = onHoldText
                )
            }
        }
        
        item {
            StatusChip(
                text = "Featured",
                icon = Icons.Default.Star,
                backgroundColor = featuredBg,
                textColor = featuredText
            )
        }
        item {
            StatusChip(
                text = "Verified",
                icon = Icons.Default.CheckCircle,
                backgroundColor = verifiedBg,
                textColor = verifiedText
            )
        }
        item {
            StatusChip(
                text = "Owner",
                backgroundColor = ownerBg,
                textColor = ownerText
            )
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    icon: ImageVector? = null,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PriceSection(
    property: PropertyModel,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineVariantColor: Color,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    val context = LocalContext.current
    var offerPrice by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = property.price,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = onBackgroundColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = onSurfaceVariantColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = surfaceVariantColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, outlineVariantColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Make an offer",
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = offerPrice,
                    onValueChange = { offerPrice = it },
                    placeholder = {
                        Text(
                            "e.g., NPR 12,000/month",
                            color = onSurfaceVariantColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = surfaceColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedTextColor = onBackgroundColor,
                        focusedTextColor = onBackgroundColor,
                        unfocusedPlaceholderColor = onSurfaceVariantColor,
                        focusedPlaceholderColor = onSurfaceVariantColor,
                        unfocusedBorderColor = outlineVariantColor,
                        focusedBorderColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (offerPrice.isNotBlank()) {
                                    sendOfferMessage(context, property, offerPrice)
                                    offerPrice = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please enter an offer price",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Offer",
                                tint = if (offerPrice.isNotBlank()) primaryColor else onSurfaceVariantColor
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun sendOfferMessage(
    context: Context,
    property: PropertyModel,
    offerPrice: String
) {
    val repository = MessageRepositoryImpl()

    val message = "Hi, I'm interested in ${property.developer}. I'd like to make an offer of $offerPrice."

    Toast.makeText(context, "Sending offer...", Toast.LENGTH_SHORT).show()

    repository.sendQuickMessageWithPropertyAndNavigate(
        context = context,
        activity = context as Activity,
        otherUserId = property.ownerId,
        otherUserName = property.ownerName.ifBlank { property.developer },
        otherUserImage = property.ownerImageUrl,
        message = message,
        property = property
    )
}

@Composable
fun PropertyDetailsSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PropertyDetailItem(
            label = property.sqft,
            value = "Area",
            onBackgroundColor = onBackgroundColor,
            onSurfaceVariantColor = onSurfaceVariantColor
        )
        PropertyDetailItem(
            label = "${property.bedrooms} Bedroom",
            value = "Apartment",
            onBackgroundColor = onBackgroundColor,
            onSurfaceVariantColor = onSurfaceVariantColor
        )
        PropertyDetailItem(
            label = "${property.bathrooms} Bath",
            value = "Bathroom",
            onBackgroundColor = onBackgroundColor,
            onSurfaceVariantColor = onSurfaceVariantColor
        )
    }
}

@Composable
fun PropertyDetailItem(
    label: String,
    value: String,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = onSurfaceVariantColor
        )
    }
}

@Composable
fun BuildingInfoSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean
) {
    val repository = remember { NearbyPlacesRepositoryImpl() }
    var nearbyPlaces by remember { mutableStateOf<Map<PlaceType, List<NearbyPlace>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(property.latLng) {
        isLoading = true
        nearbyPlaces = repository.getNearbyPlaces(property.latLng)
        isLoading = false
    }

    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = property.developer,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = onSurfaceVariantColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = property.location,
                fontSize = 14.sp,
                color = onSurfaceVariantColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section header with subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Places",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = onBackgroundColor
            )

            if (!isLoading) {
                Text(
                    text = "from OpenStreetMap",
                    fontSize = 11.sp,
                    color = onSurfaceVariantColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            LoadingNearbyPlaces(onSurfaceVariantColor = onSurfaceVariantColor, isDarkMode = isDarkMode)
        } else {
            DisplayNearbyPlaces(
                nearbyPlaces = nearbyPlaces,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun LoadingNearbyPlaces(onSurfaceVariantColor: Color, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Finding nearby places...",
            fontSize = 13.sp,
            color = onSurfaceVariantColor
        )
    }
}

@Composable
private fun DisplayNearbyPlaces(
    nearbyPlaces: Map<PlaceType, List<NearbyPlace>>,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean
) {
    val hasAnyPlaces = nearbyPlaces.values.any { it.isNotEmpty() }

    if (!hasAnyPlaces) {
        Text(
            text = "No nearby places found in this area",
            fontSize = 13.sp,
            color = onSurfaceVariantColor,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // School
        nearbyPlaces[PlaceType.SCHOOL]?.firstOrNull()?.let { place ->
            NearbyPlaceItem(
                name = place.name,
                distance = place.formattedDistance,
                icon = Icons.Default.School,
                iconTint = Color(0xFFFF9800), // Orange
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )
        }

        // Hospital
        nearbyPlaces[PlaceType.HOSPITAL]?.firstOrNull()?.let { place ->
            NearbyPlaceItem(
                name = place.name,
                distance = place.formattedDistance,
                icon = Icons.Default.LocalHospital,
                iconTint = Color(0xFFF44336), // Red
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )
        }

        // Store
        nearbyPlaces[PlaceType.STORE]?.firstOrNull()?.let { place ->
            NearbyPlaceItem(
                name = place.name,
                distance = place.formattedDistance,
                icon = Icons.Default.Store,
                iconTint = Color(0xFF4CAF50), // Green
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun NearbyPlaceItem(
    name: String,
    distance: String,
    icon: ImageVector,
    iconTint: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = iconTint.copy(alpha = if (isDarkMode) 0.2f else 0.1f),
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = iconTint,
                modifier = Modifier
                    .padding(6.dp)
                    .size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = onBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = distance,
            fontSize = 13.sp,
            color = onSurfaceVariantColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MapPreviewSection(
    property: PropertyModel,
    onClick: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    isDarkMode: Boolean = false
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(property.latLng, 13f)
            },
            properties = MapProperties(
                mapStyleOptions = if (isDarkMode) {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } else null
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false
            )
        ) {
            Marker(
                state = MarkerState(position = property.latLng),
                title = property.developer,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f))
        )

        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View on Map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
fun ContactOwnerSection(
    property: PropertyModel,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineVariantColor: Color,
    isDarkMode: Boolean,
    successColor: Color
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, outlineVariantColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contact Property Owner",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = onBackgroundColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Owner Image
                    if (property.ownerImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(property.ownerImageUrl),
                            contentDescription = "Owner",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE0E0E0),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Owner",
                                modifier = Modifier.padding(12.dp),
                                tint = onSurfaceVariantColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = property.ownerName.ifBlank { property.developer },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = onBackgroundColor
                        )
                        Text(
                            text = "Property Owner",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val messageViewModel = MessageViewModel()
                        messageViewModel.initiateCall(
                            targetUserId = property.ownerId,
                            targetUserName = property.ownerName.ifBlank { property.developer },
                            isVideoCall = false,
                            activity = context as Activity
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(successColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Quick Messages", fontSize = 14.sp, color = onSurfaceVariantColor)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMessageButton(
                    text = "Call me back",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sendQuickMessage(
                            context = context,
                            property = property,
                            message = "Hi, I'm interested in ${property.developer}. Could you please call me back?"
                        )
                    },
                    isDarkMode = isDarkMode
                )
                QuickMessageButton(
                    text = "Still available?",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sendQuickMessage(
                            context = context,
                            property = property,
                            message = "Hello! Is this property still available for ${if (property.marketType.equals("Sell", ignoreCase = true)) "buying" else property.marketType.lowercase()}?"
                        )
                    },
                    isDarkMode = isDarkMode
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            QuickMessageButton(
                text = "Schedule a visit",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    sendQuickMessage(
                        context = context,
                        property = property,
                        message = "Hi, I'd like to schedule a visit to view ${property.developer}. When would be a good time?"
                    )
                },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Updated: ${property.formattedUpdatedTime}",
                fontSize = 12.sp,
                color = onSurfaceVariantColor
            )
            Text(
                text = property.viewsText,
                fontSize = 12.sp,
                color = onSurfaceVariantColor
            )
            Text(
                text = property.uniqueViewersText,
                fontSize = 12.sp,
                color = onSurfaceVariantColor
            )
        }
    }
}

private fun sendQuickMessage(
    context: Context,
    property: PropertyModel,
    message: String
) {
    val repository = MessageRepositoryImpl()

    Toast.makeText(context, "Sending message...", Toast.LENGTH_SHORT).show()

    repository.sendQuickMessageWithPropertyAndNavigate(
        context = context,
        activity = context as Activity,
        otherUserId = property.ownerId,
        otherUserName = property.ownerName.ifBlank { property.developer },
        otherUserImage = property.ownerImageUrl,
        message = message,
        property = property
    )
}

@Composable
fun QuickMessageButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD)
    val textColor = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF2196F3)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NotesSection(
    surfaceVariantColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean
) {
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { /* Add note */ },
        color = surfaceVariantColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Note",
                    tint = onSurfaceVariantColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceColor
                )
            }
            Text(
                text = "Add",
                fontSize = 14.sp,
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PropertyDetailsInfoSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Property Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        PropertyDetailRow("Property Type", property.propertyType, onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Total Area", property.sqft, onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Bedrooms", "${property.bedrooms}", onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Bathrooms", "${property.bathrooms}", onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Floor", property.floor, onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Furnishing", property.furnishing, onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Parking", if (property.parking) "Available" else "Not Available", onBackgroundColor, onSurfaceVariantColor)
        PropertyDetailRow("Pets Allowed", if (property.petsAllowed) "Yes" else "No", onBackgroundColor, onSurfaceVariantColor)
    }
}

@Composable
fun PropertyDetailRow(
    label: String,
    value: String,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = onSurfaceVariantColor)
        Text(
            text = value,
            fontSize = 14.sp,
            color = onBackgroundColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RentalTermsSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    val hasRentalTerms = property.utilitiesIncluded != null ||
            property.commission != null ||
            property.advancePayment != null ||
            property.securityDeposit != null ||
            property.minimumLease != null ||
            property.availableFrom != null

    if (!hasRentalTerms) {
        return // Don't show section if no rental terms
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Rental Terms",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        property.utilitiesIncluded?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Utilities", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }

        property.commission?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Commission", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }

        property.advancePayment?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Advance Payment", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }

        property.securityDeposit?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Security Deposit", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }

        property.minimumLease?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Minimum Lease", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }

        property.availableFrom?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Available From", it, onBackgroundColor, onSurfaceVariantColor)
            }
        }
    }
}

@Composable
fun AmenitiesSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceColor: Color,
    successColor: Color
) {
    // Only show if property has amenities
    if (property.amenities.isEmpty()) {
        return // Don't show section if no amenities
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Amenities",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display all amenities from the property
        property.amenities.forEach { amenity ->
            AmenityItem(
                name = amenity,
                icon = getAmenityIconForPropertyDetail(amenity),
                onSurfaceColor = onSurfaceColor,
                successColor = successColor
            )
        }
    }
}

@Composable
fun AmenityItem(
    name: String,
    icon: ImageVector,
    onSurfaceColor: Color,
    successColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = successColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            color = onSurfaceColor
        )
    }
}

@Composable
fun ReportSection(
    onReportClick: () -> Unit,
    backgroundColor: Color,
    onBackgroundColor: Color,
    isDarkMode: Boolean
) {
    val reportColor = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFD32F2F)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onReportClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Report",
                tint = reportColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Report this listing",
                fontSize = 16.sp,
                color = reportColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Help us maintain quality listings",
                fontSize = 12.sp,
                color = onBackgroundColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SimilarPropertiesSection(
    similarProperties: List<PropertyModel>,
    isLoading: Boolean,
    onPropertyClick: (PropertyModel) -> Unit,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineVariantColor: Color,
    successColor: Color,
    primaryColor: Color,
    isDarkMode: Boolean
) {
    if (similarProperties.isEmpty() && !isLoading) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Header
        Text(
            text = "Similar Properties",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "You might also be interested in",
            fontSize = 14.sp,
            color = onSurfaceVariantColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            // Loading State
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Finding similar properties...",
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
            }
        } else {
            // Horizontal scrolling list of similar properties
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(similarProperties) { property ->
                    SimilarPropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property) },
                        surfaceColor = surfaceColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineVariantColor = outlineVariantColor,
                        successColor = successColor,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun SimilarPropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineVariantColor: Color,
    successColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, outlineVariantColor)
    ) {
        Column {
            // Property Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val imageUrl = property.images.values.flatten().firstOrNull()
                    ?: "https://via.placeholder.com/600x400?text=No+Image"

                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Favorite indicator if property is saved
                if (property.isFavorite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp),
                        color = surfaceColor.copy(alpha = 0.9f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            // Property Details
            Column(modifier = Modifier.padding(12.dp)) {
                // Price
                Text(
                    text = property.price,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = successColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = property.developer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = onBackgroundColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Property specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bedrooms
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bed,
                            contentDescription = "Bedrooms",
                            modifier = Modifier.size(16.dp),
                            tint = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${property.bedrooms}",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }

                    // Bathrooms
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bathroom,
                            contentDescription = "Bathrooms",
                            modifier = Modifier.size(16.dp),
                            tint = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${property.bathrooms}",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }

                    // Area
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = "Area",
                            modifier = Modifier.size(16.dp),
                            tint = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = property.sqft,
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(14.dp),
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = property.location,
                        fontSize = 12.sp,
                        color = onSurfaceVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.BottomActionButtons(
    property: PropertyModel,
    surfaceColor: Color,
    outlineVariantColor: Color,
    successColor: Color,
    primaryColor: Color
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        color = surfaceColor,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, outlineVariantColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val messageViewModel = MessageViewModel()
                    messageViewModel.initiateCall(
                        targetUserId = property.ownerId,
                        targetUserName = property.ownerName.ifBlank { property.developer },
                        isVideoCall = false,
                        activity = context as Activity
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = successColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    // Send property card with default message
                    sendQuickMessage(
                        context = context,
                        property = property,
                        message = "Hi, I'm interested in ${property.developer}."
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Message",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun DescriptionSection(
    property: PropertyModel,
    surfaceVariantColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineVariantColor: Color
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "About this property",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = surfaceVariantColor
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, outlineVariantColor)
        ) {
            Text(
                text = property.description ?: "",
                fontSize = 14.sp,
                color = onSurfaceVariantColor,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}