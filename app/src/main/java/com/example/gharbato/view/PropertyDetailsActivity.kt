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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log

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

@OptIn(ExperimentalMaterial3Api::class)
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
        },
        containerColor = Color.White
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
            // Use the reusable components from PropertyDetailActivity
            PropertyDetailsContentReusable(
                property = property!!,
                padding = padding,
                onBack = { (context as ComponentActivity).finish() }
            )
        }
    }
}

@Composable
fun PropertyDetailsContentReusable(
    property: PropertyModel,
    padding: PaddingValues,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image Gallery Section - Reused from PropertyDetailActivity
            item {
                PropertyImageSection(
                    property = property,
                    isFavorite = property.isFavorite,
                    onFavoriteClick = { /* Handle favorite toggle if needed */ },
                    onBackClick = onBack
                )
            }

            // Status Chips - Reused from PropertyDetailActivity
            item {
                StatusChipsRow()
            }

            // Price Section - Reused from PropertyDetailActivity
            item {
                PriceSection(property = property)
            }

            // Property Details - Reused from PropertyDetailActivity
            item {
                PropertyDetailsSection(property = property)
            }

            // Building Info - Reused from PropertyDetailActivity
            item {
                BuildingInfoSection(property = property)
            }

            // Description Section - Reused from PropertyDetailActivity
            if (!property.description.isNullOrBlank()) {
                item {
                    DescriptionSection(property = property)
                }
            }

            // Map Preview - Reused from PropertyDetailActivity
            item {
                MapPreviewSection(
                    property = property,
                    onClick = {
                        // You can add map navigation here if needed
                    }
                )
            }

            // Contact Owner Section - Reused from PropertyDetailActivity
            item {
                ContactOwnerSection(property = property)
            }

            // Notes Section - Reused from PropertyDetailActivity
            item {
                NotesSection()
            }

            // Property Details Info - Reused from PropertyDetailActivity
            item {
                PropertyDetailsInfoSection(property = property)
            }

            // Rental Terms - Reused from PropertyDetailActivity
            item {
                RentalTermsSection(property = property)
            }

            // Amenities - Reused from PropertyDetailActivity
            item {
                AmenitiesSection(property = property)
            }

            // Report Section - Reused from PropertyDetailActivity
            item {
                ReportSection(
                    onReportClick = { /* Handle report if needed */ }
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Bottom Action Buttons - Reused from PropertyDetailActivity
        BottomActionButtons(property = property)
    }
}