package com.example.gharbato.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.SortOption
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled = listState.firstVisibleItemIndex > 0 ||
            listState.firstVisibleItemScrollOffset > 100

    val mapHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 300.dp,
        label = "mapHeight"
    )

    val locationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val latitude = data?.getDoubleExtra(LocationPickerActivity.RESULT_LATITUDE, 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra(LocationPickerActivity.RESULT_LONGITUDE, 0.0) ?: 0.0
            val address = data?.getStringExtra(LocationPickerActivity.RESULT_ADDRESS) ?: ""
            val radius = data?.getFloatExtra(LocationPickerActivity.RESULT_RADIUS, 5f) ?: 5f

            viewModel.searchByLocation(latitude, longitude, address, radius)
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            SearchTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { query ->
                    viewModel.updateSearchQuery(query)
                },
                onFilterClick = {
                    showFilterSheet = true
                },
                onLocationClick = {
                    val intent = Intent(context, LocationPickerActivity::class.java)
                    locationPickerLauncher.launch(intent)
                },
                onSearchClick = {
                    viewModel.performSearch()
                },
                onClearSearch = {
                    viewModel.updateSearchQuery("")
                    viewModel.clearSearch()
                },
                hasActiveSearch = uiState.searchQuery.isNotEmpty() || uiState.searchLocation != null
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Map Section
                if (mapHeight > 0.dp && uiState.properties.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mapHeight)
                    ) {
                        PropertiesMapSection(
                            properties = uiState.properties,
                            context = context,
                            onMarkerClick = { property ->
                                viewModel.selectProperty(property)
                            },
                            onMapClick = {
                                val intent = Intent(context, FullSearchMapActivity::class.java)
                                context.startActivity(intent)
                            }
                        )

                        uiState.selectedProperty?.let { property ->
                            PropertyDetailOverlay(
                                property = property,
                                onClose = {
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

                SortBar(
                    propertiesCount = uiState.properties.size,
                    currentSort = uiState.currentSort,
                    onSortClick = {
                        showSortSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Searching properties...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    uiState.error ?: "No properties found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Try adjusting your search or filters",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )

                                if (uiState.searchQuery.isNotEmpty() || uiState.searchLocation != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateSearchQuery("")
                                            viewModel.clearSearch()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2196F3)
                                        )
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    }

                    uiState.properties.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No properties found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Try different filters or search terms",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    else -> {
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

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilters = uiState.currentFilters,
            onFiltersApply = { filters ->
                viewModel.applyFilters(filters)
                showFilterSheet = false
            },
            onDismiss = {
                showFilterSheet = false
            }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.currentSort,
            onSortSelected = { sortOption ->
                viewModel.updateSort(sortOption)
                showSortSheet = false
            },
            onDismiss = {
                showSortSheet = false
            }
        )
    }
}

@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    hasActiveSearch: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text(
                        text = "Search location, property type...",
                        color = Color(0xFF9E9E9E),
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    if (hasActiveSearch) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color(0xFF757575)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedContainerColor = Color.White,
                    cursorColor = Color(0xFF2196F3)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchClick() })
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF2196F3)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFF2196F3))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filters", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onLocationClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Select Location",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Near Me", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PropertiesMapSection(
    properties: List<PropertyModel>,
    context: Context,
    onMarkerClick: (PropertyModel) -> Unit,
    onMapClick: () -> Unit
) {
    // Calculate center position based on all property locations
    val centerLocation = remember(properties) {
        if (properties.isEmpty()) {
            LatLng(27.7172, 85.3240) // Default Kathmandu
        } else {
            val avgLat = properties.map { it.latitude }.average()
            val avgLng = properties.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLocation, 11f)
    }

    // Auto-fit bounds to show all properties
    LaunchedEffect(properties) {
        if (properties.isNotEmpty()) {
            try {
                val boundsBuilder = LatLngBounds.builder()
                properties.forEach { property ->
                    boundsBuilder.include(property.latLng)
                }
                val bounds = boundsBuilder.build()
                val padding = 100 // Padding in pixels
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding)
                )
            } catch (e: Exception) {
                Log.e("PropertiesMap", "Error fitting bounds: ${e.message}")
            }
        }
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
                onMapClick()
            }
        ) {
            // Display all properties with custom price markers
            properties.forEach { property ->
                Marker(
                    state = MarkerState(position = property.latLng),
                    title = property.title,
                    snippet = "${property.location} - ${property.price}",
                    icon = CustomMarkerHelper.createPriceMarker(context, property.price),
                    onClick = {
                        onMarkerClick(property)
                        true
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
                    cameraPositionState.move(CameraUpdateFactory.zoomIn())
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
                    cameraPositionState.move(CameraUpdateFactory.zoomOut())
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

        // Full Screen Button
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

        // Property Count Badge
        if (properties.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .zIndex(1f),
                color = Color(0xFF2196F3),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${properties.size} Properties",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
fun SortBar(
    propertiesCount: Int,
    currentSort: SortOption,
    onSortClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$propertiesCount Listings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )

            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currentSort.getShortName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyList(
    properties: List<PropertyModel>,
    listState: LazyListState,
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
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(property.imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onFavoriteClick(property) },
                        modifier = Modifier.size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (property.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = if (property.isFavorite) "Remove from favorites"
                            else "Add to favorites",
                            tint = if (property.isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
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
                    modifier = Modifier.align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(property.sqft, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${property.bedrooms}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${property.bathrooms}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(property.developer, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Developer", fontSize = 12.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(property.price, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(property.location, fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp).clickable {
                            if (property.ownerId.isNotEmpty()) {
                                val intent = MessageDetailsActivity.newIntent(
                                    activity = context as Activity,
                                    otherUserId = property.ownerId,
                                    otherUserName = property.ownerName.ifBlank { property.developer },
                                    otherUserImage = property.ownerImageUrl ?: ""
                                )
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Chat With Owner",
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
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
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
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(property.price, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(property.location, fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PropertyInfoChip(Icons.Default.Home, property.sqft)
                        PropertyInfoChip(Icons.Default.Info, "${property.bedrooms} BD")
                        PropertyInfoChip(Icons.Default.Star, "${property.bathrooms} BA")
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PropertyInfoChip(icon: ImageVector, text: String) {
    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, color = Color.Gray)
        }
    }
}