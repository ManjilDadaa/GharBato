package com.example.gharbato.view

import com.example.gharbato.ui.theme.LocalGharBatoActionColors
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.MyLocation
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.utils.formatNprAmount
import com.example.gharbato.utils.formatPropertyPrice
import com.example.gharbato.utils.formatPropertyLocationSummary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
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
import kotlinx.coroutines.launch

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
    val filters = uiState.currentFilters
    val hasSearchCriteria = uiState.searchQuery.isNotBlank() ||
        uiState.searchLocation != null ||
        filters.marketType.isNotBlank() ||
        filters.propertyTypes.isNotEmpty() ||
        filters.minPrice > 0 || filters.maxPrice > 0 ||
        filters.minArea > 0 || filters.maxArea > 0 ||
        filters.bedrooms.isNotBlank() || filters.furnishing.isNotBlank() ||
        filters.parking != null || filters.petsAllowed != null ||
        filters.amenities.isNotEmpty() || filters.floor.isNotBlank() ||
        filters.rentalPeriod != "Long-term"

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding().background(MaterialTheme.colorScheme.surface)) {
                SearchTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { query ->
                        viewModel.updateSearchQuery(query)
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
                    onSearchBarFocused = { focused -> isSearchBarFocused = focused },
                    hasActiveSearch = uiState.searchQuery.isNotEmpty() || uiState.searchLocation != null,
                    onFilterClick = {
                        isSearchBarFocused = false
                        showFilterSheet = true
                    },
                    activeFilterCount = countActiveFilters(filters)
                )
                ActivePropertyFilters(
                    filters = filters,
                    locationLabel = uiState.searchLocation?.address,
                    onFiltersChange = viewModel::applyFilters,
                    onClearLocation = viewModel::clearSearch
                )
                SortBar(
                    propertiesCount = if (uiState.isLoading) null else uiState.properties.size,
                    currentSort = uiState.currentSort,
                    onSortClick = { showSortSheet = true },
                    onLocationClick = {
                        isSearchBarFocused = false
                        val intent = Intent(context, LocationPickerActivity::class.java)
                        locationPickerLauncher.launch(intent)
                    },
                    showMap = uiState.showMap,
                    onMapToggle = {
                        viewModel.clearSelectedProperty()
                        viewModel.toggleMapVisibility()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isSearchBarFocused && uiState.searchQuery.isBlank() && historyUiState.searchHistory.isNotEmpty() -> {
                    SearchHistorySection(
                        searchHistory = historyUiState.searchHistory,
                        isLoading = historyUiState.isLoading,
                        onHistoryItemClick = { history ->
                            if (history.isTextSearch()) {
                                viewModel.updateSearchQuery(history.searchQuery)
                                viewModel.performSearch()
                            } else if (history.isLocationSearch()) {
                                viewModel.searchByLocation(history.locationLat, history.locationLng, history.locationAddress, history.locationRadius)
                            }
                            isSearchBarFocused = false
                        },
                        onHistoryItemDelete = { searchHistoryViewModel.deleteSearchHistory(it.id) },
                        onClearAllClick = { searchHistoryViewModel.showClearAllDialog() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.isLoading -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Searching properties...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                uiState.properties.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No properties found", style = MaterialTheme.typography.titleLarge)
                        Text("Try another area or remove a filter to see more properties.", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        if (hasSearchCriteria) {
                            Button(onClick = { viewModel.resetFilters() }, colors = GharBatoButtonDefaults.primaryColors()) { Text("Clear search and filters") }
                        } else if (uiState.error != null) {
                            OutlinedButton(onClick = viewModel::refreshProperties, colors = GharBatoButtonDefaults.outlinedColors(), border = GharBatoButtonDefaults.outlinedBorder()) { Text("Try again") }
                        }
                    }
                }
                uiState.showMap -> {
                    PropertiesMapSection(
                        properties = uiState.properties,
                        context = context,
                        onMarkerClick = viewModel::selectProperty,
                        onMapBackgroundClick = viewModel::clearSelectedProperty,
                        onMapClick = { context.startActivity(Intent(context, FullSearchMapActivity::class.java)) },
                        isDarkMode = isDarkMode
                    )
                    uiState.selectedProperty?.let { property ->
                        PropertyDetailOverlay(
                            property = property,
                            onClose = viewModel::clearSelectedProperty,
                            onViewDetails = {
                                context.startActivity(Intent(context, PropertyDetailActivity::class.java).putExtra("propertyId", property.id))
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            isDarkMode = isDarkMode
                        )
                    }
                }
                else -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                        items(uiState.properties, key = { it.id }) { property ->
                            PropertyCard(
                                property = property,
                                onClick = {
                                    val intent = Intent(context, PropertyDetailActivity::class.java)
                                    intent.putExtra("propertyId", property.id)
                                    context.startActivity(intent)
                                },
                                onShareClick = { sharedProperty ->
                                    val shareText = buildString {
                                        append("${sharedProperty.price}\n")
                                        append("${sharedProperty.location}\n")
                                        append("${sharedProperty.bedrooms} beds / ${sharedProperty.bathrooms} baths\n")
                                        append("${sharedProperty.sqft}\n\n")
                                        append("Check out this property on GharBato!")
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_SUBJECT, "Property: ${sharedProperty.title}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Property"))
                                },
                                onReportClick = { reportedProperty ->
                                    propertyToReport = reportedProperty
                                    showReportDialog = true
                                },
                                onHideClick = { hiddenProperty ->
                                    propertyToHide = hiddenProperty
                                    showHideConfirmDialog = true
                                },
                                isDarkMode = isDarkMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
            },
            previewResultsCount = viewModel::countMatchingProperties
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
    val map = mutableMapOf("priceUnit" to "NPR")

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
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                colors = GharBatoButtonDefaults.primaryColors(),
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
                colors = GharBatoButtonDefaults.outlinedColors(),
                border = GharBatoButtonDefaults.outlinedBorder(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchBarFocused: (Boolean) -> Unit,
    hasActiveSearch: Boolean,
    onFilterClick: () -> Unit,
    activeFilterCount: Int = 0
) {
    val focusManager = LocalFocusManager.current
    val submit = { onSearchClick(); focusManager.clearFocus() }
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .onFocusChanged { onSearchBarFocused(it.isFocused) },
        placeholder = { Text("Search area or property", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            IconButton(onClick = { submit() }) {
                Icon(Icons.Default.Search, contentDescription = "Search properties")
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasActiveSearch) {
                    IconButton(onClick = { onClearSearch(); focusManager.clearFocus() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
                VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                IconButton(onClick = { focusManager.clearFocus(); onFilterClick() }) {
                    BadgedBox(badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters"
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() })
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivePropertyFilters(
    filters: PropertyFilters,
    locationLabel: String?,
    onFiltersChange: (PropertyFilters) -> Unit,
    onClearLocation: () -> Unit
) {
    val chips = buildList<Pair<String, PropertyFilters>> {
        if (filters.marketType.isNotBlank()) add(when (filters.marketType) { "Sell" -> "Buy"; "Book" -> "Stay"; else -> filters.marketType } to filters.copy(marketType = ""))
        if (filters.propertyTypes.isNotEmpty()) add(filters.propertyTypes.joinToString() to filters.copy(propertyTypes = emptySet()))
        if (filters.minPrice > 0 || filters.maxPrice > 0) {
            val label = when {
                filters.minPrice == 0 -> "Up to ${formatNprAmount(filters.maxPrice)}"
                filters.maxPrice == 0 -> "From ${formatNprAmount(filters.minPrice)}"
                else -> "${formatNprAmount(filters.minPrice)} - ${formatNprAmount(filters.maxPrice)}"
            }
            add(label to filters.copy(minPrice = 0, maxPrice = 0))
        }
        if (filters.minArea > 0 || filters.maxArea > 0) add("${filters.minArea}-${filters.maxArea.takeIf { it > 0 } ?: "Any"} sq.ft" to filters.copy(minArea = 0, maxArea = 0))
        if (filters.bedrooms.isNotBlank()) add("${filters.bedrooms} bedrooms" to filters.copy(bedrooms = ""))
        if (filters.furnishing.isNotBlank()) add(filters.furnishing to filters.copy(furnishing = ""))
        filters.parking?.let { add((if (it) "Parking" else "No parking") to filters.copy(parking = null)) }
        filters.petsAllowed?.let { add((if (it) "Pets allowed" else "No pets") to filters.copy(petsAllowed = null)) }
        if (filters.amenities.isNotEmpty()) add("${filters.amenities.size} amenities" to filters.copy(amenities = emptySet()))
        if (filters.floor.isNotBlank()) add(filters.floor to filters.copy(floor = ""))
        if (filters.rentalPeriod != "Long-term") add(filters.rentalPeriod to filters.copy(rentalPeriod = "Long-term"))
    }
    if (chips.isEmpty() && locationLabel == null) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (locationLabel != null) {
            item {
                InputChip(selected = false, onClick = onClearLocation,
                    label = { Text(locationLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove location filter", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        items(chips) { (label, updatedFilters) ->
            InputChip(selected = false, onClick = { onFiltersChange(updatedFilters) },
                label = { Text(label) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $label filter", modifier = Modifier.size(16.dp)) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
        if (chips.isNotEmpty()) item { TextButton(onClick = { onFiltersChange(PropertyFilters()) }) { Text("Clear filters") } }
    }
}

@Composable
fun PropertiesMapSection(
    properties: List<PropertyModel>,
    context: Context,
    onMarkerClick: (PropertyModel) -> Unit,
    onMapClick: () -> Unit,
    isDarkMode: Boolean,
    onMapBackgroundClick: () -> Unit = {}
) {
    val mapProperties = remember(properties) { properties.filter { it.hasValidCoordinates } }
    val centerLocation = remember(mapProperties) {
        mapProperties.firstOrNull()?.latLng ?: LatLng(27.7172, 85.3240)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLocation, 11f)
    }

    LaunchedEffect(mapProperties) {
        if (mapProperties.isNotEmpty()) {
            try {
                val boundsBuilder = LatLngBounds.builder()
                mapProperties.forEach { boundsBuilder.include(it.latLng) }
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
            } catch (e: Exception) {
                Log.e("PropertiesMap", "Error fitting bounds: ${e.message}")
            }
        }
    }

    val fabBackgroundColor = MaterialTheme.colorScheme.surface
    val fabContentColor = MaterialTheme.colorScheme.onSurface
    val badgeBackgroundColor = LocalGharBatoActionColors.current.container

    Box(modifier = Modifier.fillMaxSize()) {
        if (mapProperties.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No map locations available", style = MaterialTheme.typography.titleLarge)
                Text("Switch to the list to explore these properties.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Box
        }
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
                mapToolbarEnabled = false,
                scrollGesturesEnabled = true
            ),
            onMapClick = { onMapBackgroundClick() }
        ) {
            // Display all properties with custom price markers
            mapProperties.forEach { property ->
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
                modifier = Modifier.size(48.dp),
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
                modifier = Modifier.size(48.dp),
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
                .size(48.dp)
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
        if (mapProperties.isNotEmpty()) {
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
                        tint = LocalGharBatoActionColors.current.onContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${mapProperties.size} on map",
                        color = LocalGharBatoActionColors.current.onContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBar(
    propertiesCount: Int?,
    currentSort: SortOption,
    onSortClick: () -> Unit,
    onLocationClick: () -> Unit,
    showMap: Boolean = false,
    onMapToggle: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            propertiesCount?.let { "$it ${if (it == 1) "property" else "properties"}" } ?: "Finding properties...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onLocationClick) {
            Icon(Icons.Default.MyLocation, contentDescription = "Search near me")
        }
        IconButton(onClick = onSortClick) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort properties: ${currentSort.getShortName()}")
        }
        if (onMapToggle != null) {
            FilterChip(selected = !showMap, onClick = { if (showMap) onMapToggle() }, label = { Text("List", maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(selected = showMap, onClick = { if (!showMap) onMapToggle() }, label = { Text("Map", maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    onShareClick: (PropertyModel) -> Unit,
    onReportClick: (PropertyModel) -> Unit,
    onHideClick: (PropertyModel) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    PropertyListingCard(
        property = property,
        onClick = onClick,
        modifier = modifier,
        onShareClick = { onShareClick(property) },
        onReportClick = { onReportClick(property) },
        onHideClick = { onHideClick(property) }
    )
}

@Composable
fun PropertyDetailOverlay(
    property: PropertyModel,
    onClose: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SubcomposeAsyncImage(
                    model = property.imageUrl,
                    contentDescription = property.title,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    loading = { PropertyPhotoPlaceholder() },
                    error = { PropertyPhotoPlaceholder() }
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatPropertyPrice(property.price, property.marketType), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(property.title.ifBlank { property.propertyType }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatPropertyLocationSummary(property.location), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close property preview")
                }
            }
            PropertyCardFacts(property)
            Button(onClick = onViewDetails, colors = GharBatoButtonDefaults.primaryColors(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                Text("View property")
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
        color = MaterialTheme.colorScheme.surfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
                if (label != null) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}
