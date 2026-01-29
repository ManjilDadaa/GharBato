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
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SingleBed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import android.widget.Toast
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.ReportedProperty
import com.example.gharbato.model.ReportStatus
import com.example.gharbato.model.SortOption
import com.example.gharbato.repository.ReportPropertyRepoImpl
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.ReportViewModel
import com.google.firebase.auth.FirebaseAuth
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.gharbato.R

/**
 * Helper function to navigate to message screen with fetched user fullName from Firebase
 */
private fun navigateToMessageWithUserFetch(
    context: Context,
    activity: Activity,
    otherUserId: String,
    fallbackName: String,
    fallbackImage: String = ""
) {
    // Fetch actual user data from Firebase Users collection before navigating
    FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val actualFullName = snapshot.child("fullName").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() } ?: fallbackName
                val actualProfileImage = snapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: fallbackImage

                val intent = MessageDetailsActivity.newIntent(
                    activity = activity,
                    otherUserId = otherUserId,
                    otherUserName = actualFullName,
                    otherUserImage = actualProfileImage
                )
                activity.startActivity(intent)
            }

            override fun onCancelled(error: DatabaseError) {
                // Fall back to passed name if fetch fails
                val intent = MessageDetailsActivity.newIntent(
                    activity = activity,
                    otherUserId = otherUserId,
                    otherUserName = fallbackName,
                    otherUserImage = fallbackImage
                )
                activity.startActivity(intent)
            }
        })
}

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
    var showReportDialog by remember { mutableStateOf(false) }
    var propertyToReport by remember { mutableStateOf<PropertyModel?>(null) }
    var showHideConfirmDialog by remember { mutableStateOf(false) }
    var propertyToHide by remember { mutableStateOf<PropertyModel?>(null) }

    // Report ViewModel
    val reportViewModel = remember { ReportViewModel(ReportPropertyRepoImpl()) }

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
                                onShareClick = { property ->
                                    val shareText = buildString {
                                        append("🏠 ${property.title}\n\n")
                                        append("💰 ${property.price}\n")
                                        append("📍 ${property.location}\n")
                                        append("🛏️ ${property.bedrooms} Beds • 🚿 ${property.bathrooms} Baths\n")
                                        append("📐 ${property.sqft}\n\n")
                                        append("Check out this property on GharBato!")
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_SUBJECT, "Property: ${property.title}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Property"))
                                },
                                onReportClick = { property ->
                                    propertyToReport = property
                                    showReportDialog = true
                                },
                                onHideClick = { property ->
                                    propertyToHide = property
                                    showHideConfirmDialog = true
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

    // Report Listing Dialog
    if (showReportDialog && propertyToReport != null) {
        ReportListingDialog(
            onDismiss = {
                showReportDialog = false
                propertyToReport = null
            },
            onSubmit = { reason, details ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val property = propertyToReport!!

                val report = ReportedProperty(
                    reportId = "",
                    propertyId = property.id,
                    propertyTitle = property.title,
                    propertyImage = property.imageUrl,
                    ownerId = property.ownerId,
                    ownerName = property.ownerName.ifBlank { property.developer },
                    reportedByName = "",
                    reportedBy = currentUserId,
                    reportReason = reason,
                    reportDetails = details,
                    reportedAt = System.currentTimeMillis(),
                    status = ReportStatus.PENDING
                )

                reportViewModel.submitReport(report)
                showReportDialog = false
                propertyToReport = null
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Hide Property Confirmation Dialog
    if (showHideConfirmDialog && propertyToHide != null) {
        HidePropertyConfirmDialog(
            propertyTitle = propertyToHide!!.title,
            isDarkMode = isDarkMode,
            onConfirm = {
                viewModel.hideProperty(propertyToHide!!.id)
                showHideConfirmDialog = false
                propertyToHide = null
                Toast.makeText(context, "Property hidden from your feed", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showHideConfirmDialog = false
                propertyToHide = null
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
fun HidePropertyConfirmDialog(
    propertyTitle: String,
    isDarkMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFF3E0),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Hide This Property?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You won't see \"$propertyTitle\" in your feed anymore.",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This action cannot be undone from the app.",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hide Property",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
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
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val hintColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF9E9E9E)
    val borderColor = if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE8E8E8)
    val focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)
    val containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F7FA)
    val buttonContainerColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header
            Text(
                text = "Find Your Dream Property",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Search from thousands of listings",
                fontSize = 14.sp,
                color = hintColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                shadowElevation = if (isDarkMode) 0.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                onSearchBarFocused(focusState.isFocused)
                            },
                        placeholder = {
                            Text(
                                text = "Search location, property",
                                color = hintColor,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF757575),
                                modifier = Modifier.size(22.dp)
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
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = focusedBorderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
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

                    // Search button inside the bar
                    Surface(
                        onClick = {
                            onSearchClick()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = buttonContainerColor
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Filter Button
                Surface(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                    border = BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isDarkMode) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = buttonContainerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Filters",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }

                // Location Button
                Surface(
                    onClick = onLocationClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = buttonContainerColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Near Me",
                            tint = buttonContainerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Near Me",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = buttonContainerColor
                        )
                    }
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
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val chipBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F7FA)
    val iconColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$propertiesCount Listings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Text(
                    text = "Properties available",
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }

            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(12.dp),
                color = chipBackgroundColor,
                modifier = Modifier.height(42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = currentSort.getShortName(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(18.dp)
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
    onShareClick: (PropertyModel) -> Unit,
    onReportClick: (PropertyModel) -> Unit,
    onHideClick: (PropertyModel) -> Unit,
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
                onShareClick = { onShareClick(property) },
                onReportClick = { onReportClick(property) },
                onHideClick = { onHideClick(property) },
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
    onShareClick: (PropertyModel) -> Unit,
    onReportClick: (PropertyModel) -> Unit,
    onHideClick: (PropertyModel) -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val priceColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
    val iconButtonBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f)
    val menuBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val menuTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val marketTypeColor = when (property.marketType.lowercase()) {
        "rent" -> Color(0xFF4CAF50)
        "sell" -> Color(0xFF2196F3)
        "book" -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(property.imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top row with market type badge and action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Market type badge
                    Surface(
                        color = marketTypeColor,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = when (property.marketType.lowercase()) {
                                "sell" -> "For Sale"
                                "rent" -> "For Rent"
                                "book" -> "Booking"
                                else -> property.marketType
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { onFavoriteClick(property) },
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = iconButtonBackgroundColor,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (property.isFavorite) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    contentDescription = if (property.isFavorite) "Remove from favorites"
                                    else "Add to favorites",
                                    tint = if (property.isFavorite) Color.Red else secondaryTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box {
                            Surface(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = iconButtonBackgroundColor,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(menuBackgroundColor)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = null,
                                                tint = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF2196F3),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Share", color = menuTextColor)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onShareClick(property)
                                    }
                                )

                                HorizontalDivider(
                                    color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFEEEEEE)
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Not Interested", color = menuTextColor)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onHideClick(property)
                                    }
                                )

                                HorizontalDivider(
                                    color = if (isDarkMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFFEEEEEE)
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Report,
                                                contentDescription = null,
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Report", color = Color(0xFFD32F2F))
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReportClick(property)
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom gradient with stats
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Price on image
                        Text(
                            text = property.price,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PropertyStatChip(
                                icon = Icons.Default.SquareFoot,
                                value = property.sqft,
                                label = null,
                                iconTint = Color(0xFF4CAF50)
                            )
                            PropertyStatChip(
                                icon = Icons.Default.SingleBed,
                                value = "${property.bedrooms}",
                                label = "Beds",
                                iconTint = Color(0xFF2196F3)
                            )
                            PropertyStatChip(
                                icon = Icons.Default.Bathtub,
                                value = "${property.bathrooms}",
                                label = "Baths",
                                iconTint = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
            }

            // Content section
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = property.title.ifBlank { property.developer },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = property.location,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Chat button
                    Surface(
                        onClick = {
                            if (property.ownerId.isNotEmpty()) {
                                navigateToMessageWithUserFetch(
                                    context = context,
                                    activity = context as Activity,
                                    otherUserId = property.ownerId,
                                    fallbackName = property.ownerName.ifBlank { property.developer },
                                    fallbackImage = property.ownerImageUrl ?: ""
                                )
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = priceColor
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PropertyInfoChip(Icons.Default.SquareFoot, property.sqft, chipBackgroundColor, secondaryTextColor)
                        PropertyInfoChip(Icons.Default.SingleBed, "${property.bedrooms} BD", chipBackgroundColor, secondaryTextColor)
                        PropertyInfoChip(Icons.Default.Bathtub, "${property.bathrooms} BA", chipBackgroundColor, secondaryTextColor)
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

@Composable
fun PropertyStatChip(
    icon: ImageVector,
    value: String,
    label: String?,
    iconTint: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
                if (label != null) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}