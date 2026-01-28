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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.foundation.border
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
import androidx.compose.material3.LocalTextStyle
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

    // State for fetched owner info from Firebase Users collection
    var ownerFullName by remember { mutableStateOf(property.ownerName) }
    var ownerProfileImage by remember { mutableStateOf(property.ownerImageUrl) }

    // Fetch actual owner info from Firebase Users collection
    LaunchedEffect(property.ownerId) {
        if (property.ownerId.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("Users").child(property.ownerId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fullName = snapshot.child("fullName").getValue(String::class.java)
                        val profileImage = snapshot.child("profileImageUrl").getValue(String::class.java)

                        if (!fullName.isNullOrBlank()) {
                            ownerFullName = fullName
                        }
                        if (!profileImage.isNullOrBlank()) {
                            ownerProfileImage = profileImage
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Keep using property.ownerName as fallback
                    }
                })
        }
    }

    // Create a modified property with the fetched owner info for display
    val displayProperty = property.copy(
        ownerName = ownerFullName.ifBlank { property.ownerName.ifBlank { property.developer } },
        ownerImageUrl = ownerProfileImage
    )

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
                    ownerName = ownerFullName.ifBlank { property.developer },
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
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        isDarkMode = isDarkMode
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

                // Contact Owner Section - use displayProperty for fetched owner info
                item {
                    ContactOwnerSection(
                        property = displayProperty,
                        surfaceColor = surfaceColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineVariantColor = outlineVariantColor,
                        isDarkMode = isDarkMode,
                        successColor = successColor
                    )
                }

                // Property Details Info
                item {
                    PropertyDetailsInfoSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        isDarkMode = isDarkMode
                    )
                }

                // Rental Terms
                item {
                    RentalTermsSection(
                        property = property,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        isDarkMode = isDarkMode
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

            // Use displayProperty for fetched owner info
            BottomActionButtons(
                property = displayProperty,
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
            .height(360.dp)
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

        // Gradient overlay at top for better visibility of buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient overlay at bottom for price tag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Top Bar with Back, Favorite, and Share buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button with blur effect
            Surface(
                onClick = onBackClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = surfaceColor.copy(alpha = if (isDarkMode) 0.85f else 0.95f),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = onSurfaceColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Favorite Button
                Surface(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (isFavorite) Color.Red.copy(alpha = 0.9f) else surfaceColor.copy(alpha = if (isDarkMode) 0.85f else 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.White else onSurfaceColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Share Button
                Surface(
                    onClick = { shareProperty(context, property) },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = surfaceColor.copy(alpha = if (isDarkMode) 0.85f else 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = onSurfaceColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom section with price and image indicators
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Price tag overlay
            Text(
                text = property.price,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Market type badge
            Surface(
                color = when (property.marketType.lowercase()) {
                    "rent" -> Color(0xFF4CAF50)
                    "sell" -> Color(0xFF2196F3)
                    "book" -> Color(0xFFFF9800)
                    else -> Color.Gray
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = when (property.marketType.lowercase()) {
                        "sell" -> "For Sale"
                        "rent" -> "For Rent"
                        "book" -> "For Booking"
                        else -> property.marketType
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enhanced dot indicators with counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot indicators
                if (imagesToShow.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(minOf(imagesToShow.size, 7)) { index ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 7.dp)
                                    .background(
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                        if (imagesToShow.size > 7) {
                            Text(
                                text = "+${imagesToShow.size - 7}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Image counter chip
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${pagerState.currentPage + 1}/${imagesToShow.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Show property status first if not AVAILABLE
        if (property.propertyStatus == "SOLD") {
            item {
                EnhancedStatusChip(
                    text = "Sold",
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = soldBg,
                    textColor = soldText
                )
            }
        } else if (property.propertyStatus == "ON_HOLD") {
            item {
                EnhancedStatusChip(
                    text = "On Hold",
                    icon = Icons.Default.Schedule,
                    backgroundColor = onHoldBg,
                    textColor = onHoldText
                )
            }
        }

        item {
            EnhancedStatusChip(
                text = "Featured",
                icon = Icons.Default.Star,
                backgroundColor = featuredBg,
                textColor = featuredText
            )
        }
        item {
            EnhancedStatusChip(
                text = "Verified",
                icon = Icons.Default.Verified,
                backgroundColor = verifiedBg,
                textColor = verifiedText
            )
        }
        item {
            EnhancedStatusChip(
                text = "Owner Listed",
                icon = Icons.Default.Person,
                backgroundColor = ownerBg,
                textColor = ownerText
            )
        }
    }
}

@Composable
fun EnhancedStatusChip(
    text: String,
    icon: ImageVector? = null,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
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
    val offerAccentColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
    val cardBgColor = if (isDarkMode) Color(0xFF1B3221).copy(alpha = 0.4f) else Color(0xFFE8F5E9).copy(alpha = 0.6f)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Property Title and Location
        Text(
            text = property.developer,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = onSurfaceVariantColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = property.location,
                fontSize = 14.sp,
                color = onSurfaceVariantColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Enhanced Make an Offer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = offerAccentColor.copy(alpha = if (isDarkMode) 0.25f else 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = offerAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Make an Offer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onBackgroundColor
                        )
                        Text(
                            text = "Negotiate directly with the owner",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input field with send button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = surfaceColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (offerPrice.isNotBlank()) offerAccentColor else outlineVariantColor.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NPR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = offerAccentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = offerPrice,
                            onValueChange = { offerPrice = it },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 15.sp,
                                color = onBackgroundColor
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (offerPrice.isEmpty()) {
                                        Text(
                                            text = "Enter your offer amount",
                                            fontSize = 15.sp,
                                            color = onSurfaceVariantColor.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Send button
                        Surface(
                            onClick = {
                                if (offerPrice.isNotBlank()) {
                                    sendOfferMessage(context, property, "NPR $offerPrice")
                                    offerPrice = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please enter an offer price",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (offerPrice.isNotBlank()) offerAccentColor else outlineVariantColor.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Offer",
                                    tint = if (offerPrice.isNotBlank()) Color.White else onSurfaceVariantColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick offer suggestions
                Text(
                    text = "Quick suggestions",
                    fontSize = 12.sp,
                    color = onSurfaceVariantColor,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("10,000", "15,000", "20,000", "25,000").forEach { amount ->
                        Surface(
                            onClick = { offerPrice = amount },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = if (offerPrice == amount) offerAccentColor.copy(alpha = 0.2f) else surfaceColor,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (offerPrice == amount) offerAccentColor else outlineVariantColor.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = amount,
                                fontSize = 12.sp,
                                fontWeight = if (offerPrice == amount) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (offerPrice == amount) offerAccentColor else onSurfaceVariantColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
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
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean = false
) {
    val surfaceVariant = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EnhancedPropertyStatItem(
                icon = Icons.Default.SquareFoot,
                value = property.sqft,
                label = "Area",
                iconColor = Color(0xFF4CAF50),
                textColor = onBackgroundColor,
                secondaryColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(onSurfaceVariantColor.copy(alpha = 0.2f))
            )

            EnhancedPropertyStatItem(
                icon = Icons.Default.SingleBed,
                value = "${property.bedrooms}",
                label = "Bedrooms",
                iconColor = Color(0xFF2196F3),
                textColor = onBackgroundColor,
                secondaryColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(onSurfaceVariantColor.copy(alpha = 0.2f))
            )

            EnhancedPropertyStatItem(
                icon = Icons.Default.Bathtub,
                value = "${property.bathrooms}",
                label = "Bathrooms",
                iconColor = Color(0xFF9C27B0),
                textColor = onBackgroundColor,
                secondaryColor = onSurfaceVariantColor,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
fun EnhancedPropertyStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    textColor: Color,
    secondaryColor: Color,
    isDarkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = iconColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = secondaryColor
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
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Section header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Contact Owner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor
                    )
                    Text(
                        text = "Get in touch with the property owner",
                        fontSize = 12.sp,
                        color = onSurfaceVariantColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Owner card with enhanced design
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8F9FA),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Owner Image with border
                        Box {
                            if (property.ownerImageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(property.ownerImageUrl),
                                    contentDescription = "Owner",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, successColor, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .border(2.dp, successColor, CircleShape),
                                    color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE8F5E9),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Owner",
                                            modifier = Modifier.size(28.dp),
                                            tint = successColor
                                        )
                                    }
                                }
                            }

                            // Online indicator
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(successColor, CircleShape)
                                    .border(2.dp, surfaceColor, CircleShape)
                            )
                        }

                        Column {
                            Text(
                                text = property.ownerName.ifBlank { property.developer },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onBackgroundColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = successColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Verified Owner",
                                    fontSize = 12.sp,
                                    color = successColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Call button
                    Surface(
                        onClick = {
                            val messageViewModel = MessageViewModel()
                            messageViewModel.initiateCall(
                                targetUserId = property.ownerId,
                                targetUserName = property.ownerName.ifBlank { property.developer },
                                isVideoCall = false,
                                activity = context as Activity
                            )
                        },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = successColor,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick messages section
            Text(
                text = "Quick Messages",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = onBackgroundColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enhanced quick message buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EnhancedQuickMessageButton(
                        text = "Call me back",
                        icon = Icons.Default.PhoneCallback,
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
                    EnhancedQuickMessageButton(
                        text = "Still available?",
                        icon = Icons.Default.QuestionAnswer,
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

                EnhancedQuickMessageButton(
                    text = "Schedule a visit",
                    icon = Icons.Default.CalendarMonth,
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Property stats row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PropertyStatInfo(
                        icon = Icons.Default.Update,
                        value = property.formattedUpdatedTime,
                        label = "Updated",
                        color = onSurfaceVariantColor
                    )
                    PropertyStatInfo(
                        icon = Icons.Default.Visibility,
                        value = "${property.totalViews}",
                        label = "Views",
                        color = onSurfaceVariantColor
                    )
                    PropertyStatInfo(
                        icon = Icons.Default.People,
                        value = "${property.uniqueViewers}",
                        label = "Interested",
                        color = onSurfaceVariantColor
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyStatInfo(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EnhancedQuickMessageButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD)
    val textColor = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF1976D2)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
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

    // Debug logging
    android.util.Log.d("PropertyDetailActivity", "=== sendQuickMessage called ===")
    android.util.Log.d("PropertyDetailActivity", "Property ID: ${property.id}")
    android.util.Log.d("PropertyDetailActivity", "Property Developer: ${property.developer}")
    android.util.Log.d("PropertyDetailActivity", "Property Owner ID: ${property.ownerId}")
    android.util.Log.d("PropertyDetailActivity", "Property Price: ${property.price}")
    android.util.Log.d("PropertyDetailActivity", "Property Images: ${property.images}")

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
fun PropertyDetailsInfoSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean = false
) {
    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val surfaceVariant = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)

    Column(modifier = Modifier.padding(16.dp)) {
        // Section header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Property Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
                Text(
                    text = "Complete information about this property",
                    fontSize = 12.sp,
                    color = onSurfaceVariantColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details in a card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.Home,
                    label = "Property Type",
                    value = property.propertyType,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.SquareFoot,
                    label = "Total Area",
                    value = property.sqft,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.SingleBed,
                    label = "Bedrooms",
                    value = "${property.bedrooms}",
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.Bathtub,
                    label = "Bathrooms",
                    value = "${property.bathrooms}",
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.Stairs,
                    label = "Floor",
                    value = property.floor,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.Chair,
                    label = "Furnishing",
                    value = property.furnishing,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.LocalParking,
                    label = "Parking",
                    value = if (property.parking) "Available" else "Not Available",
                    valueColor = if (property.parking) Color(0xFF4CAF50) else Color(0xFFE57373),
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
                EnhancedPropertyDetailRow(
                    icon = Icons.Default.Pets,
                    label = "Pets Allowed",
                    value = if (property.petsAllowed) "Yes" else "No",
                    valueColor = if (property.petsAllowed) Color(0xFF4CAF50) else Color(0xFFE57373),
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
fun EnhancedPropertyDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    valueColor: Color? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onSurfaceVariantColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                color = valueColor ?: onBackgroundColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = onSurfaceVariantColor.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun RentalTermsSection(
    property: PropertyModel,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    isDarkMode: Boolean = false
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

    val primaryColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val surfaceVariant = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F9FA)

    Column(modifier = Modifier.padding(16.dp)) {
        // Section header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Rental Terms",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
                Text(
                    text = "Terms and conditions for this property",
                    fontSize = 12.sp,
                    color = onSurfaceVariantColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms in a card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                property.utilitiesIncluded?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.Bolt,
                            label = "Utilities",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                property.commission?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.Percent,
                            label = "Commission",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                property.advancePayment?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.Payment,
                            label = "Advance Payment",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                property.securityDeposit?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.Shield,
                            label = "Security Deposit",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                property.minimumLease?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Minimum Lease",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                property.availableFrom?.let {
                    if (it.isNotEmpty()) {
                        RentalTermRow(
                            icon = Icons.Default.Event,
                            label = "Available From",
                            value = it,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor,
                            showDivider = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RentalTermRow(
    icon: ImageVector,
    label: String,
    value: String,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onSurfaceVariantColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                color = onBackgroundColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = onSurfaceVariantColor.copy(alpha = 0.1f),
                thickness = 1.dp
            )
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
        // Enhanced Section Header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Similar Properties",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
                Text(
                    text = "You might also be interested in",
                    fontSize = 13.sp,
                    color = onSurfaceVariantColor
                )
            }
        }

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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column {
            // Property Image with gradient overlay
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

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Market type badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    color = when (property.marketType.lowercase()) {
                        "rent" -> Color(0xFF4CAF50)
                        "sell" -> Color(0xFF2196F3)
                        "book" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (property.marketType.lowercase()) {
                            "sell" -> "Sale"
                            else -> property.marketType
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Favorite indicator if property is saved
                if (property.isFavorite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(32.dp),
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Price on image
                Text(
                    text = property.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            // Property Details
            Column(modifier = Modifier.padding(14.dp)) {
                // Title
                Text(
                    text = property.developer,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackgroundColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Property specs with proper icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Area
                    SimilarPropertyStat(
                        icon = Icons.Default.SquareFoot,
                        value = property.sqft,
                        color = Color(0xFF4CAF50)
                    )

                    // Bedrooms
                    SimilarPropertyStat(
                        icon = Icons.Default.SingleBed,
                        value = "${property.bedrooms} BD",
                        color = Color(0xFF2196F3)
                    )

                    // Bathrooms
                    SimilarPropertyStat(
                        icon = Icons.Default.Bathtub,
                        value = "${property.bathrooms} BA",
                        color = Color(0xFF9C27B0)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
fun SimilarPropertyStat(
    icon: ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Call Button
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
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Call Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // Message Button
            Button(
                onClick = {
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
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Message",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Message",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
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