package com.example.gharbato.ui.view

import com.example.gharbato.ui.theme.LocalGharBatoActionColors
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gharbato.data.model.PlaceType
import com.example.gharbato.repository.NearbyPlacesRepositoryImpl
import com.example.gharbato.viewmodel.MapViewModel
import com.example.gharbato.viewmodel.MapViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.utils.hasPropertyCoordinates
import com.example.gharbato.view.ThemePreference
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.R
import com.google.android.gms.maps.model.MapStyleOptions

class FullMapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(NearbyPlacesRepositoryImpl())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        // Get property location from intent
        val latitude = intent.getDoubleExtra("latitude", Double.NaN)
        val longitude = intent.getDoubleExtra("longitude", Double.NaN)
        val propertyName = intent.getStringExtra("propertyName") ?: "Property"

        // Initialize ViewModel
        if (hasPropertyCoordinates(latitude, longitude)) {
            viewModel.initialize(LatLng(latitude, longitude), propertyName)
        }

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            GharBatoTheme(darkTheme = isDarkMode) {
                FullMapScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMapScreen(
    viewModel: MapViewModel,
    onBack: () -> Unit,
    isDarkMode: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasPropertyLocation = hasPropertyCoordinates(
        uiState.propertyLocation.latitude,
        uiState.propertyLocation.longitude
    )
    val availablePlaces = remember(uiState.nearbyPlaces) {
        uiState.nearbyPlaces.mapValues { (_, places) ->
            places.filter { place ->
                !place.id.startsWith("fallback_") &&
                    hasPropertyCoordinates(place.location.latitude, place.location.longitude)
            }
        }.filterValues { it.isNotEmpty() }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.propertyLocation, 14f)
    }

    val mapProperties = remember(isDarkMode) {
        MapProperties(
            mapStyleOptions = if (isDarkMode) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            } else null
        )
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nearby Places",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPropertyLocation) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = onSurfaceVariantColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Location unavailable", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "The owner has not added a map location for this property.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariantColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                return@Box
            }

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // The property marker stays distinct from nearby places.
                Marker(
                    state = MarkerState(position = uiState.propertyLocation),
                    title = uiState.propertyName,
                    snippet = "Property Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                )

                // Show filtered nearby places
                availablePlaces.filterKeys { it in uiState.selectedFilters }.forEach { (placeType, places) ->
                    places.forEach { place ->
                        val markerColor = when (placeType) {
                            PlaceType.SCHOOL -> BitmapDescriptorFactory.HUE_BLUE
                            PlaceType.HOSPITAL -> BitmapDescriptorFactory.HUE_ORANGE
                            PlaceType.STORE -> BitmapDescriptorFactory.HUE_CYAN
                            PlaceType.PARK -> BitmapDescriptorFactory.HUE_GREEN
                            PlaceType.RESTAURANT -> BitmapDescriptorFactory.HUE_YELLOW
                            PlaceType.TRANSPORT -> BitmapDescriptorFactory.HUE_VIOLET
                        }

                        Marker(
                            state = MarkerState(position = place.location),
                            title = place.name,
                            snippet = place.formattedDistance,
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                        )
                    }
                }
            }

            // Filter Chips at Bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(1f),
                color = surfaceColor,
                shadowElevation = 0.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (availablePlaces.isEmpty()) "Nearby places" else "Show nearby",
                        style = MaterialTheme.typography.titleMedium,
                        color = onSurfaceColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (availablePlaces.isEmpty()) {
                        Text(
                            text = if (uiState.isLoading) "Finding nearby places?" else "Nearby place information is unavailable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariantColor
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availablePlaces.forEach { (placeType, places) ->
                                item {
                                    LocationFilterChip(
                                        text = placeType.getDisplayName(),
                                        icon = getIconForPlaceType(placeType),
                                        isSelected = uiState.selectedFilters.contains(placeType),
                                        count = places.size,
                                        onClick = { viewModel.toggleFilter(placeType) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .padding(bottom = 120.dp)
                    .zIndex(1f)
            ) {
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            com.google.android.gms.maps.CameraUpdateFactory.zoomIn()
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = onSurfaceColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            com.google.android.gms.maps.CameraUpdateFactory.zoomOut()
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = onSurfaceColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                uiState.propertyLocation,
                                14f
                            )
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on Property",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun getIconForPlaceType(placeType: PlaceType): ImageVector {
    return when (placeType) {
        PlaceType.SCHOOL -> Icons.Default.School
        PlaceType.HOSPITAL -> Icons.Default.LocalHospital
        PlaceType.STORE -> Icons.Default.ShoppingCart
        PlaceType.PARK -> Icons.Default.Park
        PlaceType.RESTAURANT -> Icons.Default.Restaurant
        PlaceType.TRANSPORT -> Icons.Default.DirectionsBus
    }
}

@Composable
fun LocationFilterChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    count: Int = 0,
    onClick: () -> Unit
) {
    val selectedColor = LocalGharBatoActionColors.current.container
    val unselectedColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedTextColor = LocalGharBatoActionColors.current.onContainer
    val unselectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (isSelected) selectedColor else unselectedColor,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isSelected) selectedTextColor else unselectedIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (count > 0) "$text ($count)" else text,
                color = if (isSelected) selectedTextColor else unselectedTextColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
