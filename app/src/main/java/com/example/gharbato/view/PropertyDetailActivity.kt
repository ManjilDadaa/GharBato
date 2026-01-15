package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FireExtinguisher
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.ReportStatus
import com.example.gharbato.model.ReportedProperty
import com.example.gharbato.repository.MessageRepositoryImpl
import com.example.gharbato.repository.NearbyPlacesRepositoryImpl
import com.example.gharbato.repository.ReportPropertyRepoImpl
import com.example.gharbato.ui.view.FullMapActivity
import com.example.gharbato.util.PropertyViewTracker
import com.example.gharbato.viewmodel.MessageViewModel
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.example.gharbato.viewmodel.ReportViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.collections.emptyMap


private fun getCurrentUserId(): String {
    return FirebaseAuth.getInstance().currentUser?.uid ?: ""
}



class PropertyDetailActivity : ComponentActivity() {

    private val viewModel: PropertyViewModel by viewModels {
        PropertyViewModelFactory(this@PropertyDetailActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val propertyId = intent.getIntExtra("propertyId", -1)

        if (propertyId != -1) {
            viewModel.getPropertyById(propertyId)

            PropertyViewTracker.trackPropertyViewById(propertyId)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            uiState.selectedProperty?.let { property ->
                PropertyDetailScreen(
                    property = property,
                    onBack = { finish() },
                    onFavoriteToggle = { prop ->
                        viewModel.toggleFavorite(prop)
                    }
                )
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
    onBack: () -> Unit,
    onFavoriteToggle: (PropertyModel) -> Unit
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
                    reportedByName = "", // You can get this from current user's profile if available
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

    Scaffold (
        containerColor = Color.White
    ){ paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Replace the LazyColumn items in PropertyDetailScreen with this:

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Image Gallery Section
                item {
                    PropertyImageSection(
                        property = property,
                        isFavorite = property.isFavorite,
                        onFavoriteClick = {
                            onFavoriteToggle(property)
                        },
                        onBackClick = onBack
                    )
                }

                // Status Chips
                item {
                    StatusChipsRow()
                }

                // Price Section
                item {
                    PriceSection(property = property)
                }

                // Property Details
                item {
                    PropertyDetailsSection(property = property)
                }

                // Building Info
                item {
                    BuildingInfoSection(property = property)
                }

                // Description Section (NEW - show property description)
                if (!property.description.isNullOrBlank()) {
                    item {
                        DescriptionSection(property = property)
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
                        }
                    )
                }

                // Contact Owner Section
                item {
                    ContactOwnerSection(property = property)
                }

                // Notes Section
                item {
                    NotesSection()
                }

                // Property Details Info
                item {
                    PropertyDetailsInfoSection(property = property)
                }

                // Rental Terms (only show if property has rental terms)
                item {
                    RentalTermsSection(property = property)
                }

                // Amenities (only show if property has amenities)
                item {
                    AmenitiesSection(property = property)
                }

                // Report Section
                item {
                    ReportSection(
                        onReportClick = { showReportDialog = true }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            BottomActionButtons(property = property)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyImageSection(
    property: PropertyModel,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onBackClick: () -> Unit
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
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }


            Row {
                // Favorite Button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        shareProperty(context, property)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Black
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
fun StatusChipsRow() {
    LazyRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatusChip(
                text = "Featured",
                icon = Icons.Default.Star,
                backgroundColor = Color(0xFFFFECB3),
                textColor = Color(0xFFFF6F00)
            )
        }
        item {
            StatusChip(
                text = "Verified",
                icon = Icons.Default.CheckCircle,
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF4CAF50)
            )
        }
        item {
            StatusChip(
                text = "Owner",
                backgroundColor = Color(0xFFE3F2FD),
                textColor = Color(0xFF2196F3)
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
fun PriceSection(property: PropertyModel) {
    val context = LocalContext.current
    var offerPrice by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = property.price,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Make an offer",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = offerPrice,
                    onValueChange = { offerPrice = it },
                    placeholder = { Text("e.g., NPR 12,000/month") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White
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
                                tint = if (offerPrice.isNotBlank()) Color(0xFF2196F3) else Color.Gray
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
fun PropertyDetailsSection(property: PropertyModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PropertyDetailItem(label = property.sqft, value = "Area")
        PropertyDetailItem(label = "${property.bedrooms} Bedroom", value = "Apartment")
        PropertyDetailItem(label = "${property.bathrooms} Bath", value = "Bathroom")
    }
}

@Composable
fun PropertyDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}


@Composable
fun BuildingInfoSection(property: PropertyModel) {
    val repository = remember { NearbyPlacesRepositoryImpl() }
    var nearbyPlaces by remember { mutableStateOf<Map<PlaceType, List<NearbyPlace>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(property.latLng) {
        isLoading = true
        nearbyPlaces = repository.getNearbyPlaces(property.latLng)
        isLoading = false
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = property.developer,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = property.location,
                fontSize = 14.sp,
                color = Color.Gray
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
                color = Color.Black
            )

            if (!isLoading) {
                Text(
                    text = "from OpenStreetMap",
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            LoadingNearbyPlaces()
        } else {
            DisplayNearbyPlaces(nearbyPlaces)
        }
    }
}

@Composable
private fun LoadingNearbyPlaces() {
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
            color = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Finding nearby places...",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun DisplayNearbyPlaces(nearbyPlaces: Map<PlaceType, List<NearbyPlace>>) {
    val hasAnyPlaces = nearbyPlaces.values.any { it.isNotEmpty() }

    if (!hasAnyPlaces) {
        Text(
            text = "No nearby places found in this area",
            fontSize = 13.sp,
            color = Color.Gray,
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
                iconTint = Color(0xFFFF9800) // Orange
            )
        }

        // Hospital
        nearbyPlaces[PlaceType.HOSPITAL]?.firstOrNull()?.let { place ->
            NearbyPlaceItem(
                name = place.name,
                distance = place.formattedDistance,
                icon = Icons.Default.LocalHospital,
                iconTint = Color(0xFFF44336) // Red
            )
        }

        // Store
        nearbyPlaces[PlaceType.STORE]?.firstOrNull()?.let { place ->
            NearbyPlaceItem(
                name = place.name,
                distance = place.formattedDistance,
                icon = Icons.Default.Store,
                iconTint = Color(0xFF4CAF50) // Green
            )
        }
    }
}

@Composable
private fun NearbyPlaceItem(
    name: String,
    distance: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = iconTint.copy(alpha = 0.1f),
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
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = distance,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
fun MapPreviewSection(
    property: PropertyModel,
    onClick: () -> Unit
) {
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
            color = Color.White,
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
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View on Map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}




@Composable
fun ContactOwnerSection(property: PropertyModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contact Property Owner",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
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
                            color = Color(0xFFE0E0E0),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Owner",
                                modifier = Modifier.padding(12.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = property.ownerName.ifBlank { property.developer },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Property Owner",
                            fontSize = 12.sp,
                            color = Color.Gray
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
                        .background(Color(0xFF4CAF50), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Quick Messages", fontSize = 14.sp, color = Color.Gray)

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
                    }
                )
                QuickMessageButton(
                    text = "Still available?",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sendQuickMessage(
                            context = context,
                            property = property,
                            message = "Hello! Is this property still available for ${property.marketType.lowercase()}?"
                        )
                    }
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
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Updated: ${property.formattedUpdatedTime}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = property.viewsText,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = property.uniqueViewersText,
                fontSize = 12.sp,
                color = Color.Gray
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
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontSize = 14.sp,
            color = Color(0xFF2196F3),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NotesSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { /* Add note */ },
        color = Color(0xFFF5F5F5),
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
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Add",
                fontSize = 14.sp,
                color = Color(0xFF2196F3),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PropertyDetailsInfoSection(property: PropertyModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Property Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        PropertyDetailRow("Property Type", property.propertyType)
        PropertyDetailRow("Total Area", property.sqft)
        PropertyDetailRow("Bedrooms", "${property.bedrooms}")
        PropertyDetailRow("Bathrooms", "${property.bathrooms}")
        PropertyDetailRow("Floor", property.floor)
        PropertyDetailRow("Furnishing", property.furnishing)
        PropertyDetailRow("Parking", if (property.parking) "Available" else "Not Available")
        PropertyDetailRow("Pets Allowed", if (property.petsAllowed) "Yes" else "No")
    }
}

@Composable
fun PropertyDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RentalTermsSection(property: PropertyModel) {
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
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        property.utilitiesIncluded?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Utilities", it)
            }
        }

        property.commission?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Commission", it)
            }
        }

        property.advancePayment?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Advance Payment", it)
            }
        }

        property.securityDeposit?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Security Deposit", it)
            }
        }

        property.minimumLease?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Minimum Lease", it)
            }
        }

        property.availableFrom?.let {
            if (it.isNotEmpty()) {
                PropertyDetailRow("Available From", it)
            }
        }
    }
}



@Composable
fun AmenitiesSection(property: PropertyModel) {
    // Only show if property has amenities
    if (property.amenities.isEmpty()) {
        return // Don't show section if no amenities
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Amenities",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display all amenities from the property
        property.amenities.forEach { amenity ->
            AmenityItem(
                name = amenity,
                icon = getAmenityIcon(amenity)
            )
        }
    }
}

@Composable
fun AmenityItem(name: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            color = Color.Black
        )
    }
}

@Composable
fun ReportSection(
    onReportClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onReportClick),
        color = Color(0xFFFCE4EC),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Report",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Report this listing",
                fontSize = 16.sp,
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Help us maintain quality listings",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}



//fun AgentHelperSection() {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
//    ) {
//        Column(
//            modifier = Modifier.padding(20.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "Need Help Finding Property?",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .size(100.dp)
//                    .background(Color.White, CircleShape)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.SupportAgent,
//                    contentDescription = "Agent",
//                    modifier = Modifier
//                        .size(60.dp)
//                        .align(Alignment.Center),
//                    tint = Color(0xFF2196F3)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = "Our agents can help you find the perfect property,schedule visits, and handle all paperwork",
//                fontSize = 14.sp,
//                color = Color.Gray,
//                textAlign = androidx.compose.ui.text.style.TextAlign.Center
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Button(
//                onClick = { /* Request agent */ },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF2196F3)
//                ),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text(
//                    text = "Request Agent Assistance",
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//    }
//}


//
//@Composable
//fun SimilarPropertiesSection(
//    price: String,
//    details: String,
//    location: String,
//    imageUrl: String,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier.clickable { /* Navigate */ },
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Column {
//            Image(
//                painter = rememberAsyncImagePainter(imageUrl),
//                contentDescription = "Similar Property",
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp),
//                contentScale = ContentScale.Crop
//            )
//            Column(modifier = Modifier.padding(12.dp)) {
//                Text(
//                    text = price,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color(0xFF4CAF50)
//                )
//                Text(
//                    text = details,
//                    fontSize = 12.sp,
//                    color = Color.Gray
//                )
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Default.LocationOn,
//                        contentDescription = null,
//                        modifier = Modifier.size(12.dp),
//                        tint = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(
//                        text = location,
//                        fontSize = 11.sp,
//                        color = Color.Gray
//                    )
//                }
//            }
//        }
//    }
//}

@Composable
fun BoxScope.BottomActionButtons(property: PropertyModel) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        color = Color.White,
        shadowElevation = 8.dp
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
                    containerColor = Color(0xFF4CAF50)
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
                    val intent = MessageDetailsActivity.newIntent(
                        activity = context as Activity,
                        otherUserId = property.ownerId,
                        otherUserName = property.ownerName.ifBlank { property.developer }
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
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
fun DescriptionSection(property: PropertyModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "About this property",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F7FA)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = property.description ?: "",
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}