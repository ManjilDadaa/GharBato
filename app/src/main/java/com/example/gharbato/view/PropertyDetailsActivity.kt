package com.example.gharbato.view

import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.formatPropertyPrice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import java.text.SimpleDateFormat
import java.util.*
import com.example.gharbato.utils.SystemBarUtils

class PropertyDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemePreference.init(this)

        val propertyId = intent.getIntExtra("propertyId", 0)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                PropertyDetailsScreen(
                    propertyId = propertyId,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PropertyDetailsScreen(
    propertyId: Int,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var property by remember { mutableStateOf<PropertyModel?>(null) }

    LaunchedEffect(propertyId) {
        val database = FirebaseDatabase.getInstance()
        val propertiesRef = database.getReference("Property")

        propertiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                property = snapshot.children.mapNotNull { child ->
                    try {
                        child.getValue(PropertyModel::class.java)
                    } catch (e: Exception) {
                        Log.e("PropertyDetails", "Error: ${e.message}")
                        null
                    }
                }.find { it.id == propertyId }

                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PropertyDetails", "Error: ${error.message}")
                isLoading = false
            }
        })
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val successColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Property details",
                        style = MaterialTheme.typography.titleLarge,
                        color = onSurfaceColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (property == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_home_24),
                        contentDescription = null,
                        tint = onSurfaceVariantColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Property Not Found",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariantColor
                    )
                }
            }
        } else {
            PropertyDetailsContent(
                property = property!!,
                padding = padding,
                backgroundColor = backgroundColor,
                surfaceColor = surfaceColor,
                onBackgroundColor = onBackgroundColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor,
                dividerColor = dividerColor,
                successColor = successColor,
                isDarkMode = isDarkMode
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyDetailsContent(
    property: PropertyModel,
    padding: PaddingValues,
    backgroundColor: Color,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color,
    dividerColor: Color,
    successColor: Color,
    isDarkMode: Boolean
) {
    val scrollState = rememberScrollState()

    // Get all images from the property
    val allImages = (property.images.values.flatten() + property.imageUrl)
        .filter { it.isNotBlank() }
        .distinct()
    val pagerState = rememberPagerState(pageCount = { allImages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(padding)
            .verticalScroll(scrollState)
    ) {
        // Image Carousel
        if (allImages.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { page ->
                    AsyncImage(
                        model = allImages[page],
                        contentDescription = "Property Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.baseline_home_24)
                    )
                }

                // Image Counter
                if (allImages.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${allImages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Status Badge
                StatusBadge(
                    status = property.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    isDarkMode = isDarkMode
                )
            }
        } else {
            // Placeholder if no images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_home_24),
                    contentDescription = null,
                    tint = onSurfaceVariantColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Property Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title and Price
            Text(
                text = property.title.ifBlank { property.propertyType },
                style = MaterialTheme.typography.headlineMedium,
                color = onBackgroundColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatPropertyPrice(property.price, property.marketType),
                style = MaterialTheme.typography.headlineSmall,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.baseline_location_on_24),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = property.location,
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatItem(
                        icon = R.drawable.baseline_bed_24,
                        value = "${property.bedrooms}",
                        label = "Bedrooms",
                        primaryColor = primaryColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = dividerColor
                    )
                    QuickStatItem(
                        icon = R.drawable.baseline_bathtub_24,
                        value = "${property.bathrooms}",
                        label = "Bathrooms",
                        primaryColor = primaryColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = dividerColor
                    )
                    QuickStatItem(
                        icon = R.drawable.baseline_square_foot_24,
                        value = property.sqft,
                        label = "Sqft",
                        primaryColor = primaryColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Property Details Section
            SectionHeader(
                "Property Details",
                onBackgroundColor = onBackgroundColor
            )
            PropertyDetailCard(
                property = property,
                surfaceColor = surfaceColor,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            if (!property.description.isNullOrEmpty()) {
                SectionHeader(
                    "Description",
                    onBackgroundColor = onBackgroundColor
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = property.description!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onSurfaceVariantColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Amenities
            if (property.amenities.isNotEmpty()) {
                SectionHeader(
                    "Amenities",
                    onBackgroundColor = onBackgroundColor
                )
                AmenitiesCard(
                    amenities = property.amenities,
                    surfaceColor = surfaceColor,
                    onBackgroundColor = onBackgroundColor,
                    successColor = successColor
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Owner Information
            SectionHeader(
                "Owner Information",
                onBackgroundColor = onBackgroundColor
            )
            OwnerInfoCard(
                property = property,
                surfaceColor = surfaceColor,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Information
            SectionHeader(
                "Additional Information",
                onBackgroundColor = onBackgroundColor
            )
            AdditionalInfoCard(
                property = property,
                surfaceColor = surfaceColor,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier, isDarkMode: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (backgroundColor, textColor) = when (status) {
        PropertyStatus.APPROVED -> colors.primaryContainer to colors.onPrimaryContainer
        PropertyStatus.REJECTED -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun QuickStatItem(
    icon: Int,
    value: String,
    label: String,
    primaryColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = onSurfaceVariantColor
        )
    }
}

@Composable
fun SectionHeader(title: String, onBackgroundColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = onBackgroundColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PropertyDetailCard(
    property: PropertyModel,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(
                "Property Type",
                property.propertyType,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Market Type",
                if (property.marketType.equals("Sell", ignoreCase = true)) "Buy" else property.marketType,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Floor",
                property.floor,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Furnishing",
                property.furnishing,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Parking",
                if (property.parking) "Available" else "Not Available",
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Pets Allowed",
                if (property.petsAllowed) "Yes" else "No",
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            if (property.kitchen.isNotEmpty()) {
                DetailRow(
                    "Kitchens",
                    property.kitchen,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (property.totalRooms.isNotEmpty()) {
                DetailRow(
                    "Total Rooms",
                    property.totalRooms,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.developer.isNullOrEmpty()) {
                DetailRow(
                    "Developer",
                    property.developer,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
        }
    }
}

@Composable
fun AmenitiesCard(
    amenities: List<String>,
    surfaceColor: Color,
    onBackgroundColor: Color,
    successColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            amenities.chunked(2).forEach { rowAmenities ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowAmenities.forEach { amenity ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_check_circle_24),
                                contentDescription = null,
                                tint = successColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = amenity,
                                fontSize = 13.sp,
                                color = onBackgroundColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun OwnerInfoCard(
    property: PropertyModel,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Owner Avatar
            if (property.ownerImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = property.ownerImageUrl,
                    contentDescription = "Owner",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(30.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.baseline_person_24)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_person_24),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = property.ownerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = property.ownerEmail,
                    fontSize = 13.sp,
                    color = onSurfaceVariantColor
                )
            }
        }
    }
}

@Composable
fun AdditionalInfoCard(
    property: PropertyModel,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!property.utilitiesIncluded.isNullOrEmpty()) {
                DetailRow(
                    "Utilities Included",
                    property.utilitiesIncluded!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.commission.isNullOrEmpty()) {
                DetailRow(
                    "Commission",
                    property.commission!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.advancePayment.isNullOrEmpty()) {
                DetailRow(
                    "Advance Payment",
                    property.advancePayment!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.securityDeposit.isNullOrEmpty()) {
                DetailRow(
                    "Security Deposit",
                    property.securityDeposit!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.minimumLease.isNullOrEmpty()) {
                DetailRow(
                    "Minimum Lease",
                    property.minimumLease!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }
            if (!property.availableFrom.isNullOrEmpty()) {
                DetailRow(
                    "Available From",
                    property.availableFrom!!,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }

            // Posted Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val postedDate = dateFormat.format(Date(property.createdAt))
            DetailRow(
                "Posted On",
                postedDate,
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )

            // Views
            DetailRow(
                "Total Views",
                "${property.totalViews}",
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Today's Views",
                "${property.todayViews}",
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
            DetailRow(
                "Unique Viewers",
                "${property.uniqueViewers}",
                onBackgroundColor = onBackgroundColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = onSurfaceVariantColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = onBackgroundColor,
            modifier = Modifier.weight(1f)
        )
    }
}
