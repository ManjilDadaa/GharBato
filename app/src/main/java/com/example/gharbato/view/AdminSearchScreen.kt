package com.example.gharbato.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.AdminSearchViewModel
import com.example.gharbato.viewmodel.AdminSearchViewModelFactory

// Modern color palette for Search Screen
private val PrimaryBlue = Color(0xFF1E88E5)
private val DarkBlue = Color(0xFF1565C0)
private val AccentGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val PendingOrange = Color(0xFFFF9800)
private val DarkOrange = Color(0xFFF57C00)
private val RejectedRed = Color(0xFFE53935)
private val BackgroundGray = Color(0xFFF5F7FA)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Custom Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBlue, DarkBlue)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Property Search",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Search by title, location, or owner",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar inside header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            placeholder = {
                                Text(
                                    "Search users, location...",
                                    color = TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = PrimaryBlue
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        isSearching = false
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = PrimaryBlue
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Status Tabs
            if (isSearching && searchQuery.length >= 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = PrimaryBlue,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .padding(horizontal = 16.dp),
                                    height = 3.dp,
                                    color = PrimaryBlue
                                )
                            }
                        },
                        divider = {}
                    ) {
                        SearchTab(
                            selected = selectedTab == 0,
                            text = "All",
                            count = uiState.allProperties.size,
                            color = PrimaryBlue,
                            onClick = { selectedTab = 0 }
                        )
                        SearchTab(
                            selected = selectedTab == 1,
                            text = "Pending",
                            count = uiState.pendingProperties.size,
                            color = PendingOrange,
                            onClick = { selectedTab = 1 }
                        )
                        SearchTab(
                            selected = selectedTab == 2,
                            text = "Approved",
                            count = uiState.approvedProperties.size,
                            color = AccentGreen,
                            onClick = { selectedTab = 2 }
                        )
                        SearchTab(
                            selected = selectedTab == 3,
                            text = "Rejected",
                            count = uiState.rejectedProperties.size,
                            color = RejectedRed,
                            onClick = { selectedTab = 3 }
                        )
                    }
                }
            }

            // Results
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryBlue,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Searching...",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    !isSearching || searchQuery.length < 2 -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = PrimaryBlue.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                if (searchQuery.isEmpty()) "Start Searching" else "Keep Typing...",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isEmpty()) "Search for properties by title, location, or owner"
                                else "Type at least 2 characters to search",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(RejectedRed.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = RejectedRed
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Something went wrong",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                uiState.error ?: "Unknown error",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
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
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(TextSecondary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HomeWork,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = TextSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    "No Properties Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Try a different search term",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
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
private fun SearchTab(
    selected: Boolean,
    text: String,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Text(
                text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color else TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) color.copy(alpha = 0.15f) else BackgroundGray)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) color else TextSecondary
                )
            }
        }
    }
}

@Composable
fun PropertySearchResults(properties: List<PropertyModel>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(properties) { property ->
            PropertySearchCard(property)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PropertySearchCard(property: PropertyModel) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (property.status) {
        "APPROVED" -> AccentGreen
        "PENDING" -> PendingOrange
        "REJECTED" -> RejectedRed
        else -> Gray
    }

    val statusGradient = when (property.status) {
        "APPROVED" -> listOf(AccentGreen, DarkGreen)
        "PENDING" -> listOf(PendingOrange, DarkOrange)
        "REJECTED" -> listOf(RejectedRed, Color(0xFFC62828))
        else -> listOf(Gray, Gray)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = statusColor.copy(alpha = 0.1f),
                spotColor = statusColor.copy(alpha = 0.1f)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column {
            // Property Image
            Box {
                if (property.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = property.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Gray.copy(alpha = 0.1f),
                                        Gray.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Gray.copy(alpha = 0.5f)
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.horizontalGradient(colors = statusGradient)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        property.status,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                // Title and Owner
                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "by ${property.ownerName}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(12.dp))

                // Price and Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            property.price,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    // Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            property.location,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Property Details Row
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PropertyDetailChip(Icons.Default.Bed, "${property.bedrooms} Bed")
                        PropertyDetailChip(Icons.Default.Bathtub, "${property.bathrooms} Bath")
                        PropertyDetailChip(Icons.Default.SquareFoot, property.sqft)
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = BackgroundGray)
                        Spacer(Modifier.height(16.dp))

                        // Owner Details Section
                        Text(
                            "Owner Details",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (property.ownerImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = property.ownerImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        property.ownerName.firstOrNull()?.toString() ?: "?",
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    property.ownerName.ifEmpty { "Unknown" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    property.ownerEmail.ifEmpty { "No email" },
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    "ID: ${property.ownerId}",
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Property Info Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                SearchInfoRow("Type", property.propertyType)
                                SearchInfoRow("Market", property.marketType)
                                SearchInfoRow("Floor", property.floor)
                                SearchInfoRow("Furnishing", property.furnishing)
                                SearchInfoRow("Parking", if (property.parking) "Yes" else "No")
                                SearchInfoRow("Pets Allowed", if (property.petsAllowed) "Yes" else "No")
                            }
                        }

                        if (property.description?.isNotEmpty() == true) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Description",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                property.description ?: "",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Property ID Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Property ID:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BackgroundGray)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "#${property.id}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (expanded) "Less" else "More",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    SearchActionButton(
                        text = "View",
                        icon = Icons.Default.Visibility,
                        gradientColors = listOf(PrimaryBlue, DarkBlue),
                        onClick = { /* View full details */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyDetailChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun SearchActionButton(
    text: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
