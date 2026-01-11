package com.example.gharbato.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: PropertyFilters,
    onFiltersApply: (PropertyFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var filters by remember { mutableStateOf(currentFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val emptyFilters = PropertyFilters()
                            filters = emptyFilters
                            onFiltersApply(emptyFilters)
                        }
                    ) {
                        Text("Reset All", color = Blue, fontWeight = FontWeight.Medium)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Divider(color = Gray.copy(0.3f))

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                FilterSection(title = "Purpose") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Buy", "Rent", "Book").forEach { type ->
                            FilterChip(
                                selected = filters.marketType == type,
                                onClick = {
                                    // ✅ Toggle: Click again to deselect
                                    filters = filters.copy(
                                        marketType = if (filters.marketType == type) "" else type
                                    )
                                },
                                label = {
                                    Text(
                                        when (type) {
                                            "Buy" -> "Buy"
                                            "Rent" -> "Rent"
                                            "Book" -> "Short-term"
                                            else -> type
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (filters.marketType.isNotEmpty()) {
                        Text(
                            "Tap again to deselect",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Rental Period (only for Rent)
                if (filters.marketType == "Rent") {
                    FilterSection(title = "Rental Period") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Long-term", "Short-term").forEach { period ->
                                FilterChip(
                                    selected = filters.rentalPeriod == period,
                                    onClick = {
                                        filters = filters.copy(
                                            rentalPeriod = if (filters.rentalPeriod == period) "" else period
                                        )
                                    },
                                    label = { Text(period, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Blue.copy(0.15f),
                                        selectedLabelColor = Blue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Property Type
                FilterSection(title = "Property Type") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Apartment", "House", "Villa", "Studio", "Commercial", "Land").forEach { type ->
                            FilterChip(
                                selected = filters.propertyTypes.contains(type),
                                onClick = {
                                    filters = filters.copy(
                                        propertyTypes = if (filters.propertyTypes.contains(type))
                                            filters.propertyTypes - type
                                        else
                                            filters.propertyTypes + type
                                    )
                                },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue.copy(0.15f),
                                    selectedLabelColor = Blue
                                )
                            )
                        }
                    }
                }

                // Price Range
                FilterSection(title = "Price Range") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (filters.minPrice > 0) "Min: रु ${filters.minPrice * 1000}" else "No minimum",
                                fontSize = 14.sp,
                                color = Gray
                            )
                            Text(
                                if (filters.maxPrice > 0) "Max: रु ${filters.maxPrice * 1000}" else "No maximum",
                                fontSize = 14.sp,
                                color = Gray
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = if (filters.minPrice > 0) filters.minPrice.toString() else "",
                                onValueChange = {
                                    filters = filters.copy(minPrice = it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Min (thousands)") },
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = if (filters.maxPrice > 0) filters.maxPrice.toString() else "",
                                onValueChange = {
                                    filters = filters.copy(maxPrice = it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Max (thousands)") },
                                placeholder = { Text("Any") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                // Bedrooms
                FilterSection(title = "Bedrooms") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Studio", "1", "2", "3", "4", "5", "6+").forEach { bed ->
                            FilterChip(
                                selected = filters.bedrooms == bed,
                                onClick = {
                                    filters = filters.copy(bedrooms = if (filters.bedrooms == bed) "" else bed)
                                },
                                label = { Text(bed) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue.copy(0.15f),
                                    selectedLabelColor = Blue
                                )
                            )
                        }
                    }
                }

                // Furnishing
                FilterSection(title = "Furnishing") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Fully Furnished", "Semi Furnished", "Unfurnished").forEach { furn ->
                            FilterChip(
                                selected = filters.furnishing == furn,
                                onClick = {
                                    filters = filters.copy(furnishing = if (filters.furnishing == furn) "" else furn)
                                },
                                label = { Text(furn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue.copy(0.15f),
                                    selectedLabelColor = Blue
                                )
                            )
                        }
                    }
                }

                // Additional Features
                FilterSection(title = "Additional Features") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filters = filters.copy(
                                        parking = when (filters.parking) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        }
                                    )
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Parking Available", fontSize = 15.sp)
                            TriStateCheckbox(
                                state = when (filters.parking) {
                                    true -> androidx.compose.ui.state.ToggleableState.On
                                    false -> androidx.compose.ui.state.ToggleableState.Off
                                    null -> androidx.compose.ui.state.ToggleableState.Indeterminate
                                },
                                onClick = {
                                    filters = filters.copy(
                                        parking = when (filters.parking) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        }
                                    )
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Blue,
                                    uncheckedColor = Gray
                                )
                            )
                        }

                        if (filters.marketType == "Rent" || filters.marketType == "Book") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        filters = filters.copy(
                                            petsAllowed = when (filters.petsAllowed) {
                                                null -> true
                                                true -> false
                                                false -> null
                                            }
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pets Allowed", fontSize = 15.sp)
                                TriStateCheckbox(
                                    state = when (filters.petsAllowed) {
                                        true -> androidx.compose.ui.state.ToggleableState.On
                                        false -> androidx.compose.ui.state.ToggleableState.Off
                                        null -> androidx.compose.ui.state.ToggleableState.Indeterminate
                                    },
                                    onClick = {
                                        filters = filters.copy(
                                            petsAllowed = when (filters.petsAllowed) {
                                                null -> true
                                                true -> false
                                                false -> null
                                            }
                                        )
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Blue,
                                        uncheckedColor = Gray
                                    )
                                )
                            }
                        }
                    }
                }

                // Amenities
                FilterSection(title = "Amenities") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Air Conditioning",
                            "WiFi Internet",
                            "Washing Machine",
                            "Refrigerator",
                            "Security",
                            "Elevator",
                            "Swimming Pool",
                            "Gym"
                        ).forEach { amenity ->
                            FilterChip(
                                selected = filters.amenities.contains(amenity),
                                onClick = {
                                    filters = filters.copy(
                                        amenities = if (filters.amenities.contains(amenity))
                                            filters.amenities - amenity
                                        else
                                            filters.amenities + amenity
                                    )
                                },
                                label = { Text(amenity, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue.copy(0.15f),
                                    selectedLabelColor = Blue
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }

            // Bottom Action Buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeFilterCount = countActiveFilters(filters)

                    Button(
                        onClick = {
                            onFiltersApply(filters)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (activeFilterCount > 0)
                                "Apply $activeFilterCount Filter${if (activeFilterCount > 1) "s" else ""}"
                            else
                                "Apply Filters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun countActiveFilters(filters: PropertyFilters): Int {
    var count = 0
    if (filters.marketType.isNotEmpty()) count++
    if (filters.propertyTypes.isNotEmpty()) count++
    if (filters.minPrice > 0 || filters.maxPrice > 0) count++
    if (filters.bedrooms.isNotEmpty()) count++
    if (filters.furnishing.isNotEmpty()) count++
    if (filters.parking != null) count++
    if (filters.petsAllowed != null) count++
    if (filters.amenities.isNotEmpty()) count++
    if (filters.floor.isNotEmpty()) count++
    return count
}

@Composable
fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}