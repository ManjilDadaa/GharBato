package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.repository.PendingPropertiesRepoImpl
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.LightGreen
import com.example.gharbato.viewmodel.PendingPropertiesViewModel
import kotlinx.coroutines.flow.collectLatest

class PendingListingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PendingListingsBody()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingListingsBody() {
    // Initialize ViewModel manually
    val viewModel = remember { PendingPropertiesViewModel(PendingPropertiesRepoImpl()) }

    // Collect state
    val uiState by viewModel.uiState.collectAsState()

    var expandedCard by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as Activity

    // Show toast messages
    LaunchedEffect(Unit) {
        viewModel.uiState.collectLatest { state ->
            state.successMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
            state.error?.let { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Listings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, AdminActivity::class.java))
                        activity.finish()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGreen)
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.properties.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        contentDescription = "No Pending",
                        modifier = Modifier.size(80.dp),
                        tint = LightGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pending Listings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All properties have been reviewed",
                        fontSize = 14.sp,
                        color = Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    item {
                        PendingHeaderCard(total = uiState.properties.size)
                    }

                    // Listing cards
                    items(uiState.properties) { listing ->
                        PendingListingCard(
                            listing = listing,
                            isExpanded = expandedCard == listing.id.toString(),
                            onExpandToggle = {
                                expandedCard = if (expandedCard == listing.id.toString()) null else listing.id.toString()
                            },
                            onApprove = {
                                viewModel.approveProperty(listing.id)
                            },
                            onReject = {
                                viewModel.rejectProperty(listing.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingHeaderCard(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Blue.copy(0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_pending_actions_24),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Blue
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Total Pending", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("$total properties waiting for review", fontSize = 13.sp, color = Gray)
            }
        }
    }
}

@Composable
fun PendingListingCard(
    listing: PropertyModel,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }

    // Reject dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Listing?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to reject '${listing.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject()
                    },
                    colors = ButtonDefaults.buttonColors(Color.Red)
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Approve dialog
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Listing?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to approve '${listing.title}'? It will be visible to all users.") },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveDialog = false
                        onApprove()
                    },
                    colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
                ) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Listing card UI
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column {
            // Images
            if (listing.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(listing.images.values.flatten()) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                // User Profile Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // User Avatar
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = LightGreen.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = listing.ownerName.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightGreen
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // User Info
                        Column {
                            Text(
                                text = listing.ownerName.ifEmpty { "Unknown User" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Posted by",
                                fontSize = 12.sp,
                                color = Gray
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Title
                Text(
                    listing.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Price
                Text(
                    listing.price,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightGreen,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Property Type & Market Type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PropertyChip(label = listing.propertyType)
                    PropertyChip(label = listing.marketType)
                }

                // Location
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_location_on_24),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        listing.location,
                        fontSize = 14.sp,
                        color = Gray
                    )
                }

                // Property Details Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PropertyDetailItem(
                        icon = R.drawable.baseline_bedroom_child_24,
                        value = "${listing.bedrooms} Beds"
                    )
                    PropertyDetailItem(
                        icon = R.drawable.baseline_bathroom_24,
                        value = "${listing.bathrooms} Baths"
                    )
                    if (listing.sqft.isNotEmpty()) {
                        PropertyDetailItem(
                            icon = R.drawable.baseline_language_24,
                            value = listing.sqft
                        )
                    }
                }

                // Developer
                if (listing.developer.isNotEmpty()) {
                    Text(
                        "Developer: ${listing.developer}",
                        fontSize = 14.sp,
                        color = Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Additional Details (Collapsible)
                if (isExpanded) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PropertyInfoRow("Floor", listing.floor)
                    PropertyInfoRow("Furnishing", listing.furnishing)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PropertyInfoChip(
                            label = if (listing.parking) "Parking Available" else "No Parking",
                            color = if (listing.parking) Color(0xFF4CAF50) else Color.Red
                        )
                        PropertyInfoChip(
                            label = if (listing.petsAllowed) "Pets Allowed" else "No Pets",
                            color = if (listing.petsAllowed) Color(0xFF4CAF50) else Color.Red
                        )
                    }

                    if (!listing.description.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Description",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            listing.description!!,
                            fontSize = 13.sp,
                            color = Gray
                        )
                    }

                    if (listing.amenities.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Amenities",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        listing.amenities.chunked(2).forEach { rowAmenities ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowAmenities.forEach { amenity ->
                                    Text(
                                        "• $amenity",
                                        fontSize = 13.sp,
                                        color = Gray,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Expand/Collapse button
                TextButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isExpanded) "Show Less" else "Show More",
                        color = LightGreen
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = LightGreen
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.1f),
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }

                Button(
                    onClick = { showApproveDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}

// Helper Composables
@Composable
fun PropertyChip(label: String) {
    Surface(
        color = LightGreen.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = LightGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PropertyDetailItem(icon: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Gray
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = Gray
        )
    }
}

@Composable
fun PropertyInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = Gray
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PropertyInfoChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}