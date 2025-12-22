package com.example.gharbato.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class FullMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get property location from intent
        val latitude = intent.getDoubleExtra("latitude", 27.7172)
        val longitude = intent.getDoubleExtra("longitude", 85.3240)
        val propertyName = intent.getStringExtra("propertyName") ?: "Property"

        setContent {
            FullMapScreen(
                propertyLocation = LatLng(latitude, longitude),
                propertyName = propertyName,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMapScreen(
    propertyLocation: LatLng,
    propertyName: String,
    onBack: () -> Unit
) {
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(propertyLocation, 14f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nearby Places",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // Property Location Marker (Home Icon)
                Marker(
                    state = MarkerState(position = propertyLocation),
                    title = propertyName,
                    snippet = "Property Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                // Nearby Places based on filters
                if (selectedFilters.contains("Schools")) {
                    // Sample school locations (replace with actual data)
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude + 0.005,
                            propertyLocation.longitude + 0.003
                        ),
                        title = "ABC School",
                        snippet = "500 m away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude - 0.003,
                            propertyLocation.longitude + 0.005
                        ),
                        title = "XYZ International School",
                        snippet = "800 m away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                }

                if (selectedFilters.contains("Hospitals")) {
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude + 0.007,
                            propertyLocation.longitude - 0.002
                        ),
                        title = "City Hospital",
                        snippet = "1.2 km away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                if (selectedFilters.contains("Stores")) {
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude - 0.002,
                            propertyLocation.longitude - 0.003
                        ),
                        title = "Supermarket",
                        snippet = "300 m away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                    )
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude + 0.003,
                            propertyLocation.longitude - 0.004
                        ),
                        title = "Shopping Mall",
                        snippet = "600 m away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                    )
                }

                if (selectedFilters.contains("Parks")) {
                    NearbyMarker(
                        position = LatLng(
                            propertyLocation.latitude - 0.006,
                            propertyLocation.longitude + 0.002
                        ),
                        title = "City Park",
                        snippet = "900 m away",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
            }


            // Filter Chips at Bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(1f),
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Show nearby",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            LocationFilterChip(
                                text = "Schools",
                                icon = Icons.Default.School,
                                isSelected = selectedFilters.contains("Schools"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Schools")) {
                                        selectedFilters - "Schools"
                                    } else {
                                        selectedFilters + "Schools"
                                    }
                                }
                            )
                        }

                        item {
                            LocationFilterChip(
                                text = "Hospitals",
                                icon = Icons.Default.LocalHospital,
                                isSelected = selectedFilters.contains("Hospitals"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Hospitals")) {
                                        selectedFilters - "Hospitals"
                                    } else {
                                        selectedFilters + "Hospitals"
                                    }
                                }
                            )
                        }

                        item {
                            LocationFilterChip(
                                text = "Stores",
                                icon = Icons.Default.ShoppingCart,
                                isSelected = selectedFilters.contains("Stores"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Stores")) {
                                        selectedFilters - "Stores"
                                    } else {
                                        selectedFilters + "Stores"
                                    }
                                }
                            )
                        }

                        item {
                            LocationFilterChip(
                                text = "Parks",
                                icon = Icons.Default.Park,
                                isSelected = selectedFilters.contains("Parks"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Parks")) {
                                        selectedFilters - "Parks"
                                    } else {
                                        selectedFilters + "Parks"
                                    }
                                }
                            )
                        }

                        item {
                            LocationFilterChip(
                                text = "Restaurants",
                                icon = Icons.Default.Restaurant,
                                isSelected = selectedFilters.contains("Restaurants"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Restaurants")) {
                                        selectedFilters - "Restaurants"
                                    } else {
                                        selectedFilters + "Restaurants"
                                    }
                                }
                            )
                        }

                        item {
                            LocationFilterChip(
                                text = "Transport",
                                icon = Icons.Default.DirectionsBus,
                                isSelected = selectedFilters.contains("Transport"),
                                onClick = {
                                    selectedFilters = if (selectedFilters.contains("Transport")) {
                                        selectedFilters - "Transport"
                                    } else {
                                        selectedFilters + "Transport"
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.move(
                            com.google.android.gms.maps.CameraUpdateFactory.zoomIn()
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.Black
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
                    containerColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // My Location Button
                FloatingActionButton(
                    onClick = {
                        // Center on property location
                        cameraPositionState.move(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                propertyLocation,
                                14f
                            )
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on Property",
                        tint = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}

    @Composable
    fun NearbyMarker(
        position: LatLng,
        title: String,
        snippet: String,
        icon: com.google.android.gms.maps.model.BitmapDescriptor
    ) {
        Marker(
            state = MarkerState(position = position),
            title = title,
            snippet = snippet,
            icon = icon
        )
    }

    @Composable
    fun LocationFilterChip(
        text: String,
        icon: ImageVector,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFF5F5F5),
            shape = RoundedCornerShape(20.dp),
            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFE0E0E0)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isSelected) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = if (isSelected) Color.White else Color.Black,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }


@Preview(showBackground = true)
@Composable
fun FullMapScreenPreview() {
    FullMapScreen(
        propertyLocation = LatLng(27.7172, 85.3240),
        propertyName = "Modern Apartment",
        onBack = {}
    )
}