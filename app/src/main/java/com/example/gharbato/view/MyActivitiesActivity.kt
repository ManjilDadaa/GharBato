package com.example.gharbato.view

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.gharbato.repository.UserRepoImpl
import android.util.Log

class MyActivitiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyActivitiesScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivitiesScreen() {
    val context = LocalContext.current
    val userRepo = remember { UserRepoImpl() }
    val currentUserId = userRepo.getCurrentUserId()

    var isLoading by remember { mutableStateOf(true) }
    var totalListings by remember { mutableStateOf(0) }
    var approvedListings by remember { mutableStateOf(0) }
    var pendingListings by remember { mutableStateOf(0) }
    var rejectedListings by remember { mutableStateOf(0) }

    // Load properties from Firebase
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            Log.d("MyActivities", "Loading properties for user: $currentUserId")

            val database = FirebaseDatabase.getInstance()
            // IMPORTANT: Changed from "Properties" to "Property" to match your Firebase structure
            val propertiesRef = database.getReference("Property")

            propertiesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("MyActivities", "Total properties in Firebase: ${snapshot.childrenCount}")

                    // Get all properties and filter by ownerId
                    val allProperties = snapshot.children.mapNotNull { child ->
                        try {
                            child.getValue(PropertyModel::class.java)
                        } catch (e: Exception) {
                            Log.e("MyActivities", "Error parsing property: ${e.message}")
                            null
                        }
                    }

                    Log.d("MyActivities", "Parsed ${allProperties.size} properties")

                    // Filter properties belonging to current user
                    val userProperties = allProperties.filter { property ->
                        property.ownerId == currentUserId
                    }

                    Log.d("MyActivities", "User has ${userProperties.size} properties")

                    // Count by status
                    totalListings = userProperties.size
                    approvedListings = userProperties.count { it.status == PropertyStatus.APPROVED }
                    pendingListings = userProperties.count { it.status == PropertyStatus.PENDING }
                    rejectedListings = userProperties.count { it.status == PropertyStatus.REJECTED }

                    Log.d("MyActivities", "Stats - Total: $totalListings, Approved: $approvedListings, Pending: $pendingListings, Rejected: $rejectedListings")

                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyActivities", "Database error: ${error.message}")
                    isLoading = false
                }
            })
        } else {
            Log.e("MyActivities", "Current user ID is null")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Activities", fontWeight = FontWeight.Bold) },
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FB))
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Total Listings Summary Card
                ActivitySectionHeader("Listing Overview")
                TotalListingsCard(totalListings)

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics Cards
                if (totalListings > 0) {
                    ActivitySectionHeader("Listing Statistics")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Approved",
                            count = approvedListings,
                            color = Color(0xFF4CAF50),
                            icon = R.drawable.baseline_check_24,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Pending",
                            count = pendingListings,
                            color = Color(0xFFFF9800),
                            icon = R.drawable.baseline_pending_actions_24,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Rejected",
                            count = rejectedListings,
                            color = Color(0xFFD32F2F),
                            icon = R.drawable.baseline_close_24,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total",
                            count = totalListings,
                            color = Blue,
                            icon = R.drawable.baseline_home_24,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed Breakdown
                    ActivitySectionHeader("Status Breakdown")
                    StatusBreakdownCard(
                        approved = approvedListings,
                        pending = pendingListings,
                        rejected = rejectedListings,
                        total = totalListings
                    )
                } else {
                    // Empty State
                    Spacer(modifier = Modifier.height(32.dp))
                    EmptyStateCard {
                        context.startActivity(Intent(context, ListingActivity::class.java))
                        (context as ComponentActivity).finish()
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/* -------------------- UI COMPONENTS -------------------- */

@Composable
fun ActivitySectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2C2C2C),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
    )
}

@Composable
fun TotalListingsCard(totalListings: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Blue.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_home_24),
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Total Listings",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$totalListings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (totalListings == 0) "No properties listed yet"
                    else if (totalListings == 1) "1 property listed"
                    else "$totalListings properties listed",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                title,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusBreakdownCard(
    approved: Int,
    pending: Int,
    rejected: Int,
    total: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Property Status Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Approved
            StatusBreakdownItem(
                label = "Approved & Live",
                count = approved,
                total = total,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pending
            StatusBreakdownItem(
                label = "Pending Review",
                count = pending,
                total = total,
                color = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rejected
            StatusBreakdownItem(
                label = "Rejected",
                count = rejected,
                total = total,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun StatusBreakdownItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label,
                    fontSize = 14.sp,
                    color = Color(0xFF2C2C2C)
                )
            }

            Text(
                "$count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "${(percentage * 100).toInt()}% of total",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun EmptyStateCard(onAddListing: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_home_24),
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "No Listings Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "You haven't listed any properties yet.\nStart by adding your first listing!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddListing,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add Your First Listing",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}