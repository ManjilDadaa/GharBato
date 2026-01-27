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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.gharbato.viewmodel.SearchHistoryViewModel
import com.example.gharbato.viewmodel.SearchHistoryViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.MapStyleOptions
import com.example.gharbato.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    val searchHistoryViewModel: SearchHistoryViewModel = viewModel(
        factory = SearchHistoryViewModelFactory()
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyUiState by searchHistoryViewModel.uiState.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var isSearchBarFocused by remember { mutableStateOf(false) }

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

            searchHistoryViewModel.saveLocationSearch(
                latitude = latitude,
                longitude = longitude,
                address = address,
                radius = radius,
                resultsCount = uiState.properties.size,
                filters = convertFiltersToMap(uiState.currentFilters)
            )
        }
    }

    Scaffold(
        containerColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White,
        topBar = {
            SearchTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { query ->
                    viewModel.updateSearchQuery(query)
                    isSearchBarFocused = query.isNotEmpty()
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
                    isSearchBarFocused = false

                    if (uiState.searchQuery.isNotEmpty()) {
                        searchHistoryViewModel.saveTextSearch(
                            query = uiState.searchQuery,
                            resultsCount = uiState.properties.size,
                            filters = convertFiltersToMap(uiState.currentFilters)
                        )
                    }
                },
                onClearSearch = {
                    viewModel.updateSearchQuery("")
                    viewModel.clearSearch()
                    isSearchBarFocused = false
                },
                onSearchBarFocused = { focused ->
                    isSearchBarFocused = focused
                },
                hasActiveSearch = uiState.searchQuery.isNotEmpty() || uiState.searchLocation != null,
                isDarkMode = isDarkMode
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSearchBarFocused && uiState.properties.isEmpty() && !uiState.isLoading) {
                    SearchHistorySection(
                        searchHistory = historyUiState.searchHistory,
                        isLoading = historyUiState.isLoading,
                        onHistoryItemClick = { history ->
                            if (history.isTextSearch()) {
                                viewModel.updateSearchQuery(history.searchQuery)
                                viewModel.performSearch()
                            } else if (history.isLocationSearch()) {
                                viewModel.searchByLocation(
                                    latitude = history.locationLat,
                                    longitude = history.locationLng,
                                    address = history.locationAddress,
                                    radiusKm = history.locationRadius
                                )
                            }
                            isSearchBarFocused = false
                        },
                        onHistoryItemDelete = { history ->
                            searchHistoryViewModel.deleteSearchHistory(history.id)
                        },
                        onClearAllClick = {
                            searchHistoryViewModel.showClearAllDialog()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                                },
                                isDarkMode = isDarkMode
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
                                        .padding(16.dp),
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }

                    SortBar(
                        propertiesCount = uiState.properties.size,
                        currentSort = uiState.currentSort,
                        onSortClick = {
                            showSortSheet = true
                        },
                        isDarkMode = isDarkMode
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
                                    CircularProgressIndicator(color = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Searching properties...",
                                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Gray
                                    )
                                }
                            }
                        }

                        uiState.error != null && uiState.properties.isEmpty() -> {
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
                                        tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No results found",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Try adjusting your search or filters",
                                        fontSize = 14.sp,
                                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                    )

                                    if (uiState.searchQuery.isNotEmpty() || uiState.searchLocation != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                viewModel.updateSearchQuery("")
                                                viewModel.clearSearch()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
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
                                        tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No properties found",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Try different filters or search terms",
                                        fontSize = 14.sp,
                                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
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
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Sheet
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

    // Sort Sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.currentSort,
            onSortSelected = { sortOption ->
                viewModel.updateSort(sortOption)
                showSortSheet = false
            },
            onDismiss = {
                showSortSheet = false
            },
            isDarkMode = isDarkMode
        )
    }

    // Clear History Dialog
    if (historyUiState.showClearAllDialog) {
        ClearHistoryDialog(
            onConfirm = {
                searchHistoryViewModel.clearAllSearchHistory()
            },
            onDismiss = {
                searchHistoryViewModel.hideClearAllDialog()
            }
        )
    }
}

// Helper function to convert PropertyFilters to Map for storage
private fun convertFiltersToMap(filters: com.example.gharbato.model.PropertyFilters): Map<String, String> {
    val map = mutableMapOf<String, String>()

    if (filters.marketType.isNotBlank()) {
        map["marketType"] = filters.marketType
    }
    if (filters.propertyTypes.isNotEmpty()) {
        map["propertyTypes"] = filters.propertyTypes.joinToString(",")
    }
    if (filters.minPrice > 0) {
        map["minPrice"] = filters.minPrice.toString()
    }
    if (filters.maxPrice > 0) {
        map["maxPrice"] = filters.maxPrice.toString()
    }
    if (filters.bedrooms.isNotBlank()) {
        map["bedrooms"] = filters.bedrooms
    }

    return map
}

@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchBarFocused: (Boolean) -> Unit,
    hasActiveSearch: Boolean,
    isDarkMode: Boolean
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color.White
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val hintColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF9E9E9E)
    val borderColor = if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0)
    val focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAFAFA)
    val buttonBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val buttonContainerColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = backgroundColor,
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
                    .height(56.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        onSearchBarFocused(focusState.isFocused)
                    },
                placeholder = {
                    Text(
                        text = "Search location, property type...",
                        color = hintColor,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575),
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    if (hasActiveSearch) {
                        IconButton(onClick = {
                            onClearSearch()
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = focusedBorderColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                    unfocusedContainerColor = containerColor,
                    cursorColor = focusedBorderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = hintColor,
                    unfocusedLabelColor = hintColor,
                    focusedPlaceholderColor = hintColor,
                    unfocusedPlaceholderColor = hintColor,
                    focusedLeadingIconColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575),
                    unfocusedLeadingIconColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575),
                    focusedTrailingIconColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575),
                    unfocusedTrailingIconColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearchClick()
                        focusManager.clearFocus()
                    }
                )
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
                        contentColor = buttonBorderColor
                    ),
                    border = BorderStroke(1.5.dp, buttonBorderColor)
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
                        containerColor = buttonContainerColor,
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
    onMapClick: () -> Unit,
    isDarkMode: Boolean
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

    val fabBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val fabContentColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val badgeBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = if (isDarkMode) {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } else null
            ),
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
                containerColor = fabBackgroundColor
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = fabContentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FloatingActionButton(
                onClick = {
                    cameraPositionState.move(CameraUpdateFactory.zoomOut())
                },
                modifier = Modifier.size(40.dp),
                containerColor = fabBackgroundColor
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = fabContentColor
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
            containerColor = fabBackgroundColor
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Full Screen",
                tint = fabContentColor
            )
        }

        // Property Count Badge
        if (properties.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .zIndex(1f),
                color = badgeBackgroundColor,
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
    onSortClick: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val chipBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
    val chipTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val iconColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
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
                color = textColor
            )

            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(20.dp),
                color = chipBackgroundColor,
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
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currentSort.getShortName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = chipTextColor
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
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
    onFavoriteClick: (PropertyModel) -> Unit,
    isDarkMode: Boolean
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
                onFavoriteClick = { onFavoriteClick(property) },
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    onFavoriteClick: (PropertyModel) -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current

    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val priceColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
    val chipBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
    val overlayBackgroundColor = if (isDarkMode) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
    val iconButtonBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
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
                            .background(iconButtonBackgroundColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (property.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = if (property.isFavorite) "Remove from favorites"
                            else "Add to favorites",
                            tint = if (property.isFavorite) Color.Red else secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                            .background(iconButtonBackgroundColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .background(overlayBackgroundColor, RoundedCornerShape(topEnd = 12.dp))
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
                        Text(property.developer, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("Developer", fontSize = 12.sp, color = secondaryTextColor)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(property.price, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = priceColor)

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = secondaryTextColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(property.location, fontSize = 12.sp, color = secondaryTextColor)
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = priceColor,
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
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val priceColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
    val chipBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
    val buttonColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
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
                    Text(property.price, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = priceColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = secondaryTextColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(property.location, fontSize = 14.sp, color = secondaryTextColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PropertyInfoChip(Icons.Default.Home, property.sqft, chipBackgroundColor, secondaryTextColor)
                        PropertyInfoChip(Icons.Default.Info, "${property.bedrooms} BD", chipBackgroundColor, secondaryTextColor)
                        PropertyInfoChip(Icons.Default.Star, "${property.bathrooms} BA", chipBackgroundColor, secondaryTextColor)
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = secondaryTextColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PropertyInfoChip(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(color = backgroundColor, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, color = contentColor)
        }
    }
}