package com.example.gharbato.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.MapStyleOptions
import com.example.gharbato.R
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
import kotlinx.coroutines.launch
import java.util.Locale

class MapLocationPickerActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Fine location permission granted
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Coarse location permission granted
            }
            else -> {
                // No location permission granted
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ThemePreference.init(this)

        // Get current location from intent (for editing)
        val currentLatitude = intent.getDoubleExtra("CURRENT_LATITUDE", 27.7172)
        val currentLongitude = intent.getDoubleExtra("CURRENT_LONGITUDE", 85.3240)

        // Check and request location permissions
        checkLocationPermission()

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)

            GharBatoTheme(darkTheme = isDarkMode) {
                MapLocationPickerScreen(
                    initialLatitude = currentLatitude,
                    initialLongitude = currentLongitude,
                    onLocationSelected = { lat, lng, address ->
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_LATITUDE, lat)
                            putExtra(RESULT_LONGITUDE, lng)
                            putExtra(RESULT_ADDRESS, address)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    fusedLocationClient = fusedLocationClient,
                    isDarkMode = isDarkMode
                )
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    companion object {
        const val RESULT_LATITUDE = "latitude"
        const val RESULT_LONGITUDE = "longitude"
        const val RESULT_ADDRESS = "address"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    initialLatitude: Double = 27.7172,
    initialLongitude: Double = 85.3240,
    onLocationSelected: (Double, Double, String) -> Unit,
    onCancel: () -> Unit,
    fusedLocationClient: FusedLocationProviderClient,
    isDarkMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Theme colors
    val surfaceColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val onSurfaceColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val onSurfaceVariantColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val primaryColor = if (isDarkMode) Color(0xFF82B1FF) else Color(0xFF2196F3)
    val cardColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)

    // Initialize with provided location or default
    var selectedLocation by remember { mutableStateOf(LatLng(initialLatitude, initialLongitude)) }
    var selectedAddress by remember { mutableStateOf("Select a location") }
    var isLoading by remember { mutableStateOf(false) }

    // Use rememberMarkerState for draggable marker
    val markerState = rememberMarkerState(position = selectedLocation)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 15f)
    }

    // Update marker position when selectedLocation changes
    LaunchedEffect(selectedLocation) {
        markerState.position = selectedLocation
    }

    // Update address when location changes
    LaunchedEffect(selectedLocation) {
        scope.launch {
            isLoading = true
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(
                    selectedLocation.latitude,
                    selectedLocation.longitude,
                    1
                )
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    selectedAddress = buildString {
                        if (address.featureName != null) append(address.featureName)
                        if (address.locality != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.locality)
                        }
                        if (address.subAdminArea != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.subAdminArea)
                        }
                        if (isEmpty()) {
                            append("${selectedLocation.latitude}, ${selectedLocation.longitude}")
                        }
                    }
                }
            } catch (e: Exception) {
                selectedAddress = "${selectedLocation.latitude}, ${selectedLocation.longitude}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Select Property Location",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Tap and drag to adjust",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surfaceColor,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Address Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = cardColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Location",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = primaryColor
                                    )
                                } else {
                                    Text(
                                        text = selectedAddress,
                                        fontSize = 14.sp,
                                        color = onSurfaceColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lat: %.6f, Lng: %.6f".format(
                                        selectedLocation.latitude,
                                        selectedLocation.longitude
                                    ),
                                    fontSize = 11.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Button
                    Button(
                        onClick = {
                            onLocationSelected(
                                selectedLocation.latitude,
                                selectedLocation.longitude,
                                selectedAddress
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Confirm Location",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapStyleOptions = if (isDarkMode) {
                        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                    } else null
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                ),
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLng(latLng),
                            400
                        )
                    }
                }
            ) {
                // Fixed: Use rememberMarkerState and observe position changes
                Marker(
                    state = markerState,
                    title = "Property Location",
                    draggable = true
                )
            }

            // Observe marker drag to update selectedLocation
            LaunchedEffect(markerState.position) {
                if (markerState.position != selectedLocation) {
                    selectedLocation = markerState.position
                }
            }

            // Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomIn(),
                                300
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = onSurfaceColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomOut(),
                                300
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = onSurfaceColor
                    )
                }
            }

            // Current Location Button
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        try {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                    location?.let {
                                        val currentLatLng = LatLng(it.latitude, it.longitude)
                                        selectedLocation = currentLatLng
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f),
                                                500
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp),
                containerColor = surfaceColor,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = primaryColor
                )
            }

            // Help Card
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColor.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap anywhere on the map or drag the marker to set exact location",
                        fontSize = 12.sp,
                        color = onSurfaceColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}