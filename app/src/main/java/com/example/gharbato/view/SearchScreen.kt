package com.example.gharbato.ui.view

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.repository.RepositoryProvider
import com.example.gharbato.view.CustomMarkerHelper
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(
            RepositoryProvider.getPropertyRepository(),
            RepositoryProvider.getSavedPropertiesRepository()
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val isScrolled = listState.firstVisibleItemIndex > 0 ||
            listState.firstVisibleItemScrollOffset > 100

    val mapHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 300.dp,
        label = "mapHeight"
    )

    Scaffold(
        topBar = {
            SearchTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) }
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
                        MapSection(
                            properties = uiState.properties,
                            context = context,
                            onMarkerClick = { property ->
                                // ✅ Show property overlay when marker clicked
                                viewModel.selectProperty(property)
                            },
                            onMapClick = {
                                // ✅ Open FullSearchMapActivity when map background clicked
                                val intent = Intent(context, FullSearchMapActivity::class.java)
                                context.startActivity(intent)
                            }
                        )

                        // ✅ Show overlay when marker is clicked
                        uiState.selectedProperty?.let { property ->
                            PropertyDetailOverlay(
                                property = property,
                                onClose = {
                                    // ✅ Clear selection to show all listings
                                    viewModel.clearSelectedProperty()
                                },
                                onViewDetails = {
                                    val intent = Intent(context, PropertyDetailActivity::class.java)
                                    intent.putExtra("propertyId", property.id)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // Filter Chips
                FilterChipsSection(
                    selectedMarketType = uiState.selectedMarketType,
                    onMarketTypeChange = { viewModel.updateMarketType(it) },
                    selectedPropertyType = uiState.selectedPropertyType,
                    onPropertyTypeChange = { viewModel.updatePropertyType(it) },
                    minPrice = uiState.minPrice,
                    onMinPriceChange = { viewModel.updateMinPrice(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Properties Count
                Text(
                    text = "${uiState.properties.size} Listings",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Loading State
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Property List
                    PropertyList(
                        properties = uiState.properties,
                        listState = listState,
                        onPropertyClick = { property ->
                            val intent = Intent(context, PropertyDetailActivity::class.java)
                            intent.putExtra("propertyId", property.id)
                            context.startActivity(intent)
                        },
                        onFavoriteClick = { property ->
                            viewModel.toggleFavorite(property)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MapSection(
    properties: List<PropertyModel>,
    context: android.content.Context,
    onMarkerClick: (PropertyModel) -> Unit,
    onMapClick: () -> Unit
) {
    val startLocation = properties.firstOrNull()?.latLng
        ?: com.google.android.gms.maps.model.LatLng(27.7172, 85.3240)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 11f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapClick = {
                // ✅ Clicking map background opens FullMapActivity
                onMapClick()
            }
        ) {
            properties.forEach { property ->
                Marker(
                    state = MarkerState(position = property.latLng),
                    title = property.price,
                    snippet = property.location,
                    icon = CustomMarkerHelper.createPriceMarker(context, property.price),
                    onClick = {
                        // ✅ Clicking marker shows overlay
                        onMarkerClick(property)
                        true // Consume the click event
                    }
                )
            }
        }

        // Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
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

        // ✅ Fullscreen button
        FloatingActionButton(
            onClick = onMapClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .zIndex(1f),
            containerColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Full Screen",
                tint = Color.Black
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
                        Text("9+", color = Color.White, fontWeight = FontWeight.Bold)
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
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPropertyClick: (PropertyModel) -> Unit,
    onFavoriteClick: (PropertyModel) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(properties) { property ->
            PropertyCard(
                property = property,
                onClick = { onPropertyClick(property) },
                onFavoriteClick = { onFavoriteClick(property) }
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    onFavoriteClick: (PropertyModel) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        onClick = { onFavoriteClick(property) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (property.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (property.isFavorite) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (property.isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { /* Handle more options */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
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
                                imageVector = Icons.AutoMirrored.Filled.Send,
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

@Composable
fun PropertyDetailOverlay(
    property: PropertyModel,
    onClose: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Image(
                    painter = rememberAsyncImagePainter(property.imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

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

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PropertyInfoChip(Icons.Default.Home, property.sqft)
                        PropertyInfoChip(Icons.Default.Info, "${property.bedrooms} BD")
                        PropertyInfoChip(Icons.Default.Star, "${property.bathrooms} BA")
                    }
                }

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

            Button(
                onClick = onViewDetails,
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
fun PropertyInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
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