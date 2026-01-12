package com.example.gharbato.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.FireExtinguisher
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.model.getDefaultAmenities
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenitiesContentScreen(
    state: PropertyListingState,
    onStateChange: (PropertyListingState) -> Unit
) {
    val allAmenities = getDefaultAmenities()
    val selectedCount = state.amenities.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            "Amenities",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Select features available in your property",
            fontSize = 14.sp,
            color = Gray
        )

        Spacer(Modifier.height(8.dp))

        // Selection counter badge
        Surface(
            color = Blue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "$selectedCount selected",
                    fontSize = 13.sp,
                    color = Blue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allAmenities.forEach { amenity ->
                val selected = state.amenities.contains(amenity)

                FilterChip(
                    selected = selected,
                    onClick = {
                        val updated = if (selected)
                            state.amenities - amenity
                        else
                            state.amenities + amenity

                        onStateChange(state.copy(amenities = updated))
                    },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getAmenityIcon(amenity),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                amenity,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue.copy(alpha = 0.12f),
                        selectedLabelColor = Blue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) Blue else Gray.copy(0.3f)
                    ),
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        InfoHint(
            text = "Amenities increase visibility and tenant interest."
        )

        Spacer(Modifier.height(80.dp))
    }
}


fun getAmenityIcon(amenityName: String): ImageVector {
    return when (amenityName.lowercase()) {
        // Climate Control
        "air conditioning", "ac", "cooling" -> Icons.Default.AcUnit

        // Internet & Connectivity
        "wifi internet", "wifi", "internet" -> Icons.Default.Wifi

        // Appliances
        "washing machine", "laundry" -> Icons.Default.LocalLaundryService
        "refrigerator", "fridge", "kitchen" -> Icons.Default.Kitchen

        // Security
        "security", "guard" -> Icons.Default.Security
        "cctv", "surveillance", "camera" -> Icons.Default.Videocam
        "alarm", "fire alarm" -> Icons.Default.Warning

        // Building Features
        "elevator", "lift" -> Icons.Default.Apartment
        "parking", "car parking", "bike parking" -> Icons.Default.LocalParking
        "garage" -> Icons.Default.Home

        // Recreation & Fitness
        "gym", "fitness center", "fitness" -> Icons.Default.FitnessCenter
        "swimming pool", "pool" -> Icons.Default.Pool
        "playground", "play area", "sports" -> Icons.Default.SportsSoccer

        // Outdoor Spaces
        "garden", "lawn", "park", "green space" -> Icons.Default.Park
        "balcony", "terrace" -> Icons.Default.Deck
        "rooftop" -> Icons.Default.Apartment

        // Utilities
        "power backup", "generator", "backup" -> Icons.Default.Power
        "water supply 24/7", "water supply", "water" -> Icons.Default.WaterDrop
        "gas", "gas connection" -> Icons.Default.LocalFireDepartment
        "solar panels", "solar" -> Icons.Default.WbSunny

        // Dining & Kitchen
        "dining area", "dining room" -> Icons.Default.Restaurant

        // Entertainment
        "tv", "television", "cable tv" -> Icons.Default.Tv
        "home theater", "theater" -> Icons.Default.Theaters
        "clubhouse", "club" -> Icons.Default.Celebration

        // Services
        "housekeeping", "cleaning" -> Icons.Default.CleaningServices
        "maintenance", "24/7 maintenance" -> Icons.Default.Build
        "concierge", "reception" -> Icons.Default.Business

        // Accessibility
        "wheelchair access", "disabled access" -> Icons.Default.Accessible

        // Pets
        "pet friendly", "pets allowed" -> Icons.Default.Pets

        // Additional Rooms
        "study room", "study" -> Icons.AutoMirrored.Filled.MenuBook
        "store room", "storage" -> Icons.Default.Inventory

        // Safety
        "fire safety", "fire extinguisher" -> Icons.Default.FireExtinguisher
        "medical facility", "medical", "first aid" -> Icons.Default.MedicalServices

        // Default icon for unrecognized amenities
        else -> Icons.Default.CheckCircle
    }
}


@Composable
fun InfoHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F9FF)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_info_24),
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp
            )
        }
    }
}