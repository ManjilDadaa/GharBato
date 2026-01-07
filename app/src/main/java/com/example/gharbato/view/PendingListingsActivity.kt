package com.example.gharbato.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

class PendingListingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PendingListingsScreen()
        }
    }
}

// Data model
data class PendingListing(
    val id: String,
    val title: String,
    val userName: String,
    val userEmail: String,
    val propertyType: String,
    val purpose: String,
    val price: String,
    val location: String,
    val bedrooms: String,
    val bathrooms: String,
    val area: String,
    val description: String,
    val images: List<String>,
    val submittedDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingListingsScreen() {
    // Sample data - Replace with Firebase data
    val pendingListings = remember {
        listOf(
            PendingListing(
                id = "1",
                title = "Luxury Villa in Kathmandu",
                userName = "John Doe",
                userEmail = "john@example.com",
                propertyType = "Villa",
                purpose = "Sell",
                price = "50,00,000",
                location = "Kathmandu, Nepal",
                bedrooms = "4",
                bathrooms = "3",
                area = "2500",
                description = "Beautiful villa with modern amenities",
                images = listOf(
                    "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800",
                    "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800"
                ),
                submittedDate = "2 hours ago"
            ),
            PendingListing(
                id = "2",
                title = "Modern Apartment Downtown",
                userName = "Jane Smith",
                userEmail = "jane@example.com",
                propertyType = "Apartment",
                purpose = "Rent",
                price = "25,000/month",
                location = "Lalitpur, Nepal",
                bedrooms = "2",
                bathrooms = "2",
                area = "1200",
                description = "Fully furnished apartment",
                images = listOf(
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800"
                ),
                submittedDate = "5 hours ago"
            )
        )
    }

    var expandedCard by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Listings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Blue.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_pending_actions_24),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Blue
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Total Pending", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${pendingListings.size} properties waiting for review", fontSize = 13.sp, color = Gray)
                        }
                    }
                }
            }

            // Listings
            items(pendingListings) { listing ->
                PendingListingCard(
                    listing = listing,
                    isExpanded = expandedCard == listing.id,
                    onExpandToggle = {
                        expandedCard = if (expandedCard == listing.id) null else listing.id
                    }
                )
            }
        }
    }
}

@Composable
fun PendingListingCard(
    listing: PendingListing,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Listing?") },
            text = { Text("Are you sure you want to reject '${listing.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Handle rejection
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(Color.Red)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Listing?") },
            text = { Text("Are you sure you want to approve '${listing.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Handle approval
                        showApproveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                    items(listing.images) { imageUrl ->
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
                // Title
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text(
                        listing.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null
                        )
                    }
                }

                // Tags row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Blue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = listing.purpose,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Rs. ${listing.price}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

// Quick info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bed,
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${listing.bedrooms} Bed",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bathtub,
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${listing.bathrooms} Bath",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${listing.area} sqft",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = listing.location,
                        fontSize = 13.sp,
                        color = Gray
                    )
                }

                // Expanded details
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("Submitted by:", fontSize = 12.sp, color = Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = listing.userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = listing.userEmail,
                            fontSize = 12.sp,
                            color = Gray
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Description:", fontSize = 12.sp, color = Gray)
                        Text(
                            text = listing.description,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Submitted: ${listing.submittedDate}",
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = SolidColor(Color.Red)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reject")
                    }

                    Button(
                        onClick = { showApproveDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Approve")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }
}