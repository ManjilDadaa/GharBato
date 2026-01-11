package com.example.gharbato.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.AdminSearchViewModel
import com.example.gharbato.viewmodel.AdminSearchViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSearchScreen() {
    val viewModel: AdminSearchViewModel = viewModel(
        factory = AdminSearchViewModelFactory()
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            viewModel.searchProperties(searchQuery)
        } else if (searchQuery.isEmpty()) {
            isSearching = false
            viewModel.clearSearch()
        }
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(Color(0xFFF5F5F5))
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Blue,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Search properties by title, location, or owner",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search users, location...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Gray)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearching = false
                                }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Status Tabs
            if (isSearching && searchQuery.length >= 2) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Blue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text("All (${uiState.allProperties.size})")
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text("Pending (${uiState.pendingProperties.size})")
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text("Approved (${uiState.approvedProperties.size})")
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text("Rejected (${uiState.rejectedProperties.size})")
                        }
                    )
                }
            }

            // Results
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    !isSearching || searchQuery.length < 2 -> {
                        // Empty state
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Gray.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isEmpty()) "Start searching" else "Type at least 2 characters",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray
                            )
                            Text(
                                "Search for properties",
                                fontSize = 14.sp,
                                color = Gray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Red
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.error ?: "Unknown error", color = Color.Red)
                        }
                    }

                    else -> {
                        val propertiesToShow = when (selectedTab) {
                            0 -> uiState.allProperties
                            1 -> uiState.pendingProperties
                            2 -> uiState.approvedProperties
                            3 -> uiState.rejectedProperties
                            else -> uiState.allProperties
                        }

                        if (propertiesToShow.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.HomeWork,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Gray.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("No properties found", color = Gray)
                            }
                        } else {
                            PropertySearchResults(propertiesToShow)
                        }
                    }
                }
            }
        }
    }
}

    @Composable
    fun PropertySearchResults(properties: List<PropertyModel>) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(properties) { property ->
                PropertySearchCard(property)
            }
        }
    }

    @Composable
    fun PropertySearchCard(property: PropertyModel) {
        var expanded by remember { mutableStateOf(false) }

        val statusColor = when (property.status) {
            "APPROVED" -> Color(0xFF4CAF50)
            "PENDING" -> Color(0xFFFF9800)
            "REJECTED" -> Color.Red
            else -> Gray
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Property Image
                if (property.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = property.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Gray
                        )
                    }
                }

                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                property.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "by ${property.ownerName}",
                                fontSize = 13.sp,
                                color = Gray
                            )
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                property.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Price and Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AttachMoney,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                property.price,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                property.location,
                                fontSize = 13.sp,
                                color = Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Property Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PropertyDetailItem(Icons.Default.Bed, "${property.bedrooms} Bed")
                        PropertyDetailItem(Icons.Default.Bathtub, "${property.bathrooms} Bath")
                        PropertyDetailItem(Icons.Default.SquareFoot, property.sqft)
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column {
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))

                            // Owner Details
                            Text(
                                "Owner Details:",
                                fontSize = 12.sp,
                                color = Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = Blue.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (property.ownerImageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = property.ownerImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                property.ownerName.firstOrNull()?.toString() ?: "?",
                                                color = Blue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                Column {
                                    Text(
                                        property.ownerName.ifEmpty { "Unknown" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        property.ownerEmail.ifEmpty { "No email" },
                                        fontSize = 12.sp,
                                        color = Gray
                                    )
                                    Text(
                                        "ID: ${property.ownerId}",
                                        fontSize = 11.sp,
                                        color = Gray.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Property Info
                            InfoRow("Type", property.propertyType)
                            InfoRow("Market", property.marketType)
                            InfoRow("Floor", property.floor)
                            InfoRow("Furnishing", property.furnishing)
                            InfoRow("Parking", if (property.parking) "Yes" else "No")
                            InfoRow("Pets Allowed", if (property.petsAllowed) "Yes" else "No")

                            if (property.description?.isNotEmpty() == true) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Description:",
                                    fontSize = 12.sp,
                                    color = Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    property.description ?: "",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Property ID
                            Text(
                                "Property ID: ${property.id}",
                                fontSize = 11.sp,
                                color = Gray
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (expanded) "Less" else "More")
                        }

                        Button(
                            onClick = { /* View full details */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue
                            )
                        ) {
                            Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("View")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PropertyDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 13.sp, color = Color.DarkGray)
        }
    }

    @Composable
    fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$label:",
                fontSize = 13.sp,
                color = Gray,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.6f)
            )
        }
    }