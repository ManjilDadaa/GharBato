package com.example.gharbato.view

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
import com.example.gharbato.ui.theme.Blue
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

class PropertyDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val propertyId = intent.getIntExtra("propertyId", 0)

        setContent {
            PropertyDetailsScreen(propertyId = propertyId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PropertyDetailsScreen(propertyId: Int) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Property Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Blue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        } else if (property == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_home_24),
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Property Not Found",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        } else {
            PropertyDetailsContent(property = property!!, padding = padding)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyDetailsContent(property: PropertyModel, padding: PaddingValues) {
    val scrollState = rememberScrollState()

    // Get all images from the property
    val allImages = property.images.values.flatten()
    val pagerState = rememberPagerState(pageCount = { allImages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
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
                        .padding(16.dp)
                )
            }
        } else {
            // Placeholder if no images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_home_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
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
                text = property.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Rs ${property.price}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Blue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.baseline_location_on_24),
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = property.location,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
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
                        label = "Bedrooms"
                    )
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                    )
                    QuickStatItem(
                        icon = R.drawable.baseline_bathtub_24,
                        value = "${property.bathrooms}",
                        label = "Bathrooms"
                    )
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                    )
                    QuickStatItem(
                        icon = R.drawable.baseline_square_foot_24,
                        value = property.sqft,
                        label = "Sqft"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Property Details Section
            SectionHeader("Property Details")
            PropertyDetailCard(property)

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            if (!property.description.isNullOrEmpty()) {
                SectionHeader("Description")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        text = property.description!!,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Amenities
            if (property.amenities.isNotEmpty()) {
                SectionHeader("Amenities")
                AmenitiesCard(amenities = property.amenities)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Property Statistics
            SectionHeader("Property Statistics")
            PropertyStatsCard(property)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (status) {
        PropertyStatus.APPROVED -> Color(0xFF4CAF50) to Color.White
        PropertyStatus.PENDING -> Color(0xFFFF9800) to Color.White
        PropertyStatus.REJECTED -> Color(0xFFD32F2F) to Color.White
        else -> Color.Gray to Color.White
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
fun QuickStatItem(icon: Int, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C2C2C)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2C2C2C),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PropertyDetailCard(property: PropertyModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow("Property Type", property.propertyType)
            DetailRow("Market Type", property.marketType)
            DetailRow("Floor", property.floor)
            DetailRow("Furnishing", property.furnishing)
            DetailRow("Parking", if (property.parking) "Available" else "Not Available")
            DetailRow("Pets Allowed", if (property.petsAllowed) "Yes" else "No")
            if (property.kitchen.isNotEmpty()) {
                DetailRow("Kitchens", property.kitchen)
            }
            if (property.totalRooms.isNotEmpty()) {
                DetailRow("Total Rooms", property.totalRooms)
            }
            if (!property.developer.isNullOrEmpty()) {
                DetailRow("Developer", property.developer)
            }
        }
    }
}

@Composable
fun AmenitiesCard(amenities: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = amenity,
                                fontSize = 13.sp,
                                color = Color(0xFF2C2C2C)
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
fun PropertyStatsCard(property: PropertyModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val postedDate = dateFormat.format(Date(property.createdAt))
            
            DetailRow("Posted On", postedDate)
            DetailRow("Total Views", "${property.totalViews}")
            DetailRow("Today's Views", "${property.todayViews}")
            DetailRow("Unique Viewers", "${property.uniqueViewers}")
            
            if (!property.utilitiesIncluded.isNullOrEmpty()) {
                DetailRow("Utilities Included", property.utilitiesIncluded!!)
            }
            if (!property.commission.isNullOrEmpty()) {
                DetailRow("Commission", property.commission!!)
            }
            if (!property.advancePayment.isNullOrEmpty()) {
                DetailRow("Advance Payment", property.advancePayment!!)
            }
            if (!property.securityDeposit.isNullOrEmpty()) {
                DetailRow("Security Deposit", property.securityDeposit!!)
            }
            if (!property.minimumLease.isNullOrEmpty()) {
                DetailRow("Minimum Lease", property.minimumLease!!)
            }
            if (!property.availableFrom.isNullOrEmpty()) {
                DetailRow("Available From", property.availableFrom!!)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C2C2C),
            modifier = Modifier.weight(1f)
        )
    }
}