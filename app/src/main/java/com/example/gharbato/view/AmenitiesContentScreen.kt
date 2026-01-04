package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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

        // ✅ Proper inline counter (fixes your UI issue)
        Text(
            "$selectedCount selected",
            fontSize = 13.sp,
            color = Blue,
            fontWeight = FontWeight.Medium
        )

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
                        Text(
                            amenity,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue.copy(alpha = 0.12f),
                        selectedLabelColor = Blue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected
                    )

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


@Composable
fun getAmenityIcon(amenity: String): Int {
    return when (amenity) {
        "Air Conditioning" -> R.drawable.baseline_ac_unit_24
        "WiFi Internet" -> R.drawable.baseline_wifi_24
        "Washing Machine" -> R.drawable.baseline_local_laundry_service_24
        "Refrigerator" -> R.drawable.baseline_kitchen_24
        "Security" -> R.drawable.baseline_security_24
        "Elevator" -> R.drawable.baseline_elevator_24
        else -> R.drawable.baseline_check_24
    }
}

@Composable
fun InfoHint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F6FF), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_info_24),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}
