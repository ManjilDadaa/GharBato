package com.example.gharbato.view

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.gharbatocopy.model.PropertyModel
import com.example.gharbatocopy.model.SampleData
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMarketType by remember { mutableStateOf("Buy") }
    var selectedPropertyType by remember { mutableStateOf("Secondary market") }
    var minPrice by remember { mutableStateOf(16) }
    var isMapFullScreen by remember { mutableStateOf(false) }
    var selectedProperty by remember { mutableStateOf<PropertyModel?>(null) }

    // Track scroll state to hide/show map
    val listState = rememberLazyListState()
    val isScrolled = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100

    // Animate map height based on scroll
    val mapHeight by animateDpAsState(
        targetValue = when {
            isMapFullScreen -> 800.dp
            isScrolled -> 0.dp
            else -> 300.dp
        },
        label = "mapHeight"
    )

    Scaffold(
        topBar = {
            // Top Bar with Search
            SearchTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Map Section
                if (mapHeight > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mapHeight)
                    ) {
                        // Google Map with custom price markers
                        MapWithPriceMarkers(
                            properties = SampleData.properties,
                            isFullScreen = isMapFullScreen,
                            context = context,
                            onMarkerClick = { property ->
                                selectedProperty = property
                            },
                            onMapClick = {
                                if (!isMapFullScreen) {
                                    isMapFullScreen = true
                                }
                            }
                        )

                        // Close button for full screen
                        if (isMapFullScreen) {
                            IconButton(
                                onClick = { isMapFullScreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .background(Color.White, CircleShape)
                                    .zIndex(10f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close full screen",
                                    tint = Color.Black
                                )
                            }
                        }

                        // Show selected property card overlay
                        selectedProperty?.let { property ->
                            PropertyDetailOverlay(
                                property = property,
                                onClose = { selectedProperty = null },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // Filter Chips Section
                FilterChipsSection(
                    selectedMarketType = selectedMarketType,
                    onMarketTypeChange = { selectedMarketType = it },
                    selectedPropertyType = selectedPropertyType,
                    onPropertyTypeChange = { selectedPropertyType = it },
                    minPrice = minPrice,
                    onMinPriceChange = { minPrice = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Properties Count
                Text(
                    text = "${SampleData.properties.size} Listing",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Property List
                PropertyList(
                    properties = SampleData.properties,
                    listState = listState
                )
            }

            // "Save search" button
            if (!isMapFullScreen && mapHeight > 0.dp && selectedProperty == null) {
                Button(
                    onClick = { /* Handle save search */ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .height(48.dp)
                        .zIndex(10f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Save search",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// Map with custom price markers
@Composable
fun MapWithPriceMarkers(
    properties: List<PropertyModel>,
    isFullScreen: Boolean,
    context: Context,
    onMarkerClick: (PropertyModel) -> Unit,
    onMapClick: () -> Unit
) {
    val startLocation = properties.firstOrNull()?.latLng
        ?: LatLng(27.7172, 85.3240)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            startLocation,
            if (isFullScreen) 12f else 11f
        )
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onMapClick() },
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        // Add custom price markers for each property
        properties.forEach { property ->
            Marker(
                state = MarkerState(position = property.latLng),
                title = property.price,
                snippet = property.location,
                icon = CustomMarkerHelper.createPriceMarker(context, property.price),
                onClick = {
                    onMarkerClick(property)
                    true
                }
            )
        }
    }

    // Zoom controls overlay
    Column(
        modifier = Modifier
            .padding(16.dp)
            .zIndex(1f)
    ) {
        FloatingActionButton(
            onClick = {
                cameraPositionState.move(
                    com.google.android.gms.maps.CameraUpdateFactory.zoomIn()
                )
            },
            modifier = Modifier.size(40.dp),
            containerColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FloatingActionButton(
            onClick = {
                cameraPositionState.move(
                    com.google.android.gms.maps.CameraUpdateFactory.zoomOut()
                )
            },
            modifier = Modifier.size(40.dp),
            containerColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = Color.Black
            )
        }
    }
}

// Property detail overlay when marker is clicked
@Composable
fun PropertyDetailOverlay(
    property: PropertyModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Property Image
                Image(
                    painter = rememberAsyncImagePainter(property.imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Property Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = property.price,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = property.location,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PropertyInfoChip(
                            icon = Icons.Default.Home,
                            text = property.sqft
                        )
                        PropertyInfoChip(
                            icon = Icons.Default.Info,
                            text = "${property.bedrooms} BD"
                        )
                        PropertyInfoChip(
                            icon = Icons.Default.Star,
                            text = "${property.bathrooms} BA"
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Details Button
            Button(
                onClick = {
                    val intent = Intent(context, PropertyDetailActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PropertyInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "What are you looking for?",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = searchQuery,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2196F3), CircleShape)
                        .clickable { /* Handle location */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsSection(
    selectedMarketType: String,
    onMarketTypeChange: (String) -> Unit,
    selectedPropertyType: String,
    onPropertyTypeChange: (String) -> Unit,
    minPrice: Int,
    onMinPriceChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedMarketType == "Buy",
                    onClick = {
                        onMarketTypeChange(if (selectedMarketType == "Buy") "Rent" else "Buy")
                    },
                    label = { Text(selectedMarketType) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE3F2FD),
                        selectedLabelColor = Color(0xFF2196F3)
                    )
                )
            }

            item {
                FilterChip(
                    selected = true,
                    onClick = { /* Handle property type */ },
                    label = { Text(selectedPropertyType) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE3F2FD),
                        selectedLabelColor = Color(0xFF2196F3)
                    )
                )
            }

            item {
                FilterChip(
                    selected = true,
                    onClick = { /* Handle bedrooms */ },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("9+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50),
                        selectedLabelColor = Color.White
                    )
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.clickable { /* Handle price */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "from $minPrice thousand",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            item {
                FilterChip(
                    selected = false,
                    onClick = { /* Handle sort */ },
                    label = { Text("Sort by") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PropertyList(
    properties: List<PropertyModel>,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(properties) { property ->
            PropertyCard(property = property)
        }
    }
}

@Composable
fun PropertyCard(property: PropertyModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, PropertyDetailActivity::class.java)
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(property.imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { /* Handle favorite */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { /* Handle more */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(topEnd = 12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = property.sqft,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.bedrooms}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.bathrooms}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = property.developer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Developer",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = property.price,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = property.location,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { /* Handle chat */ }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Chat",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchScreen() {
    SearchScreen()
}