package com.example.gharbato.view

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import com.example.gharbato.utils.SystemBarUtils
import com.example.gharbato.ui.theme.Blue

class LocationPickerActivity : ComponentActivity() {

    companion object {
        const val RESULT_LATITUDE = "latitude"
        const val RESULT_LONGITUDE = "longitude"
        const val RESULT_ADDRESS = "address"
        const val RESULT_RADIUS = "radius"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)

        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            LocationPickerScreen(
                isDarkMode = isDarkMode,
                onLocationSelected = { location, address, radius ->
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_LATITUDE, location.latitude)
                        putExtra(RESULT_LONGITUDE, location.longitude)
                        putExtra(RESULT_ADDRESS, address)
                        putExtra(RESULT_RADIUS, radius)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onCancel = {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    isDarkMode: Boolean,
    onLocationSelected: (LatLng, String, Float) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FB)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF2C2C2C)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val fabBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    val fabContentColor = if (isDarkMode) Color(0xFFE1E1E1) else Color.Black
    val primaryColor = if (isDarkMode) Color(0xFF82B1FF) else Blue
    val circleFillColor = if (isDarkMode) Color(0x4D82B1FF) else Color(0x4D2196F3)
    val circleStrokeColor = if (isDarkMode) Color(0xFF82B1FF) else Color(0xFF2196F3)
    val sliderInactiveColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE3F2FD)
    val disabledButtonColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFCCCCCC)
    val cardBorderColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)

    // Default location (Kathmandu)
    var selectedLocation by remember { mutableStateOf(LatLng(27.7172, 85.3240)) }
    var selectedAddress by remember { mutableStateOf("Kathmandu, Nepal") }
    var searchRadius by remember { mutableStateOf(5f) } // in kilometers
    var isGeocoding by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 13f)
    }

    // Update selected location when camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedLocation = cameraPositionState.position.target

            // Reverse geocode to get address
            isGeocoding = true
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(
                    selectedLocation.latitude,
                    selectedLocation.longitude,
                    1
                )
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    selectedAddress = when {
                        address.featureName != null && address.locality != null ->
                            "${address.featureName}, ${address.locality}"
                        address.locality != null -> address.locality
                        address.subAdminArea != null -> address.subAdminArea
                        address.adminArea != null -> address.adminArea
                        else -> "Unknown Location"
                    }
                }
            } catch (e: Exception) {
                selectedAddress = "Unknown Location"
            } finally {
                isGeocoding = false
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Location",
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            ) {
                // Draw circle for search radius
                Circle(
                    center = selectedLocation,
                    radius = searchRadius * 1000.0, // Convert km to meters
                    fillColor = circleFillColor,
                    strokeColor = circleStrokeColor,
                    strokeWidth = 2f
                )
            }

            // Center pin indicator
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Selected Location",
                tint = if (isDarkMode) Color(0xFFFF5252) else Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .offset(y = (-24).dp)
                    .zIndex(2f)
            )

            // Zoom Controls (Left side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .zIndex(3f)
            ) {
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            CameraUpdateFactory.zoomIn()
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = fabBackgroundColor,
                    contentColor = fabContentColor,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            CameraUpdateFactory.zoomOut()
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = fabBackgroundColor,
                    contentColor = fabContentColor,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out"
                    )
                }
            }

            // My Location button (Top right)
            FloatingActionButton(
                onClick = {
                    val defaultLocation = LatLng(27.7172, 85.3240)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(3f),
                containerColor = fabBackgroundColor,
                contentColor = primaryColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location"
                )
            }

            // Location info card at bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .zIndex(4f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkMode) 4.dp else 8.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor
                ),
                border = if (isDarkMode) CardDefaults.outlinedCardBorder() else null
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Selected Location",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isGeocoding) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Getting location...",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        }
                    } else {
                        Text(
                            text = selectedAddress,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Radius",
                            fontSize = 14.sp,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${searchRadius.toInt()} km",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Radius slider
                    Slider(
                        value = searchRadius,
                        onValueChange = { searchRadius = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = sliderInactiveColor,
                            activeTickColor = primaryColor,
                            inactiveTickColor = secondaryTextColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "1 km",
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                        Text(
                            "20 km",
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Confirm button
                    Button(
                        onClick = {
                            onLocationSelected(selectedLocation, selectedAddress, searchRadius)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isGeocoding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = disabledButtonColor,
                            contentColor = Color.White,
                            disabledContentColor = if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF757575)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = if (isGeocoding) Color(0xFF9E9E9E) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Search in this area",
                            fontWeight = FontWeight.Bold,
                            color = if (isGeocoding) Color(0xFF9E9E9E) else Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}