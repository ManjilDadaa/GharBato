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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()
    var filters by remember { mutableStateOf(currentFilters) }

    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val dividerColor = if (isDarkMode) MaterialTheme.colorScheme.outline else Gray.copy(0.3f)
    val chipContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Blue.copy(0.15f)
    val chipLabelColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Blue
    val selectedChipContainerColor = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else Blue
    val selectedChipLabelColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
    val borderColor = if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0)
    val placeholderColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Gray

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
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
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val emptyFilters = PropertyFilters()
                            filters = emptyFilters
                            onFiltersApply(emptyFilters)
                        }
                    ) {
                        Text(
                            "Reset All",
                            color = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = secondaryTextColor
                        )
                    }
                }
            }

            Divider(color = dividerColor)

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                FilterSection(title = "Purpose", isDarkMode = isDarkMode) {
                    // Display labels map to Firebase-stored values:
                    // "Buy" (user perspective) -> "Sell" (stored in Firebase)
                    // "Rent" -> "Rent", "Book" -> "Book"
                    val purposeOptions = listOf(
                        "Buy" to "Sell",   // UI label -> Firebase value
                        "Rent" to "Rent",
                        "Book" to "Book"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        purposeOptions.forEach { (displayLabel, firebaseValue) ->
                            FilterChip(
                                selected = filters.marketType == firebaseValue,
                                onClick = {
                                    filters = filters.copy(
                                        marketType = if (filters.marketType == firebaseValue) "" else firebaseValue
                                    )
                                },
                                label = {
                                    Text(
                                        when (displayLabel) {
                                            "Buy" -> "Buy"
                                            "Rent" -> "Rent"
                                            "Book" -> "Short-term"
                                            else -> displayLabel
                                        },
                                        fontWeight = FontWeight.Medium,
                                        color = if (filters.marketType == firebaseValue) selectedChipLabelColor else chipLabelColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = selectedChipContainerColor,
                                    selectedLabelColor = selectedChipLabelColor,
                                    containerColor = chipContainerColor,
                                    labelColor = chipLabelColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (filters.marketType.isNotEmpty()) {
                        Text(
                            "Tap again to deselect",
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Rental Period (only for Rent)
                if (filters.marketType == "Rent") {
                    FilterSection(title = "Rental Period", isDarkMode = isDarkMode) {
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
                                    label = {
                                        Text(
                                            period,
                                            fontWeight = FontWeight.Medium,
                                            color = if (filters.rentalPeriod == period) chipLabelColor else chipLabelColor
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipContainerColor,
                                        selectedLabelColor = chipLabelColor,
                                        containerColor = chipContainerColor,
                                        labelColor = chipLabelColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Property Type
                FilterSection(title = "Property Type", isDarkMode = isDarkMode) {
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
                                label = {
                                    Text(
                                        type,
                                        color = if (filters.propertyTypes.contains(type)) chipLabelColor else chipLabelColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipContainerColor,
                                    selectedLabelColor = chipLabelColor,
                                    containerColor = chipContainerColor,
                                    labelColor = chipLabelColor
                                )
                            )
                        }
                    }
                }

                // Price Range
                FilterSection(title = "Price Range", isDarkMode = isDarkMode) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (filters.minPrice > 0) "Min: रु ${filters.minPrice * 1000}" else "No minimum",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                            Text(
                                if (filters.maxPrice > 0) "Max: रु ${filters.maxPrice * 1000}" else "No maximum",
                                fontSize = 14.sp,
                                color = secondaryTextColor
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
                                label = { Text("Min (thousands)", color = secondaryTextColor) },
                                placeholder = { Text("0", color = placeholderColor) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = secondaryTextColor,
                                    unfocusedLabelColor = secondaryTextColor,
                                    focusedPlaceholderColor = placeholderColor,
                                    unfocusedPlaceholderColor = placeholderColor,
                                    cursorColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue
                                )
                            )

                            OutlinedTextField(
                                value = if (filters.maxPrice > 0) filters.maxPrice.toString() else "",
                                onValueChange = {
                                    filters = filters.copy(maxPrice = it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Max (thousands)", color = secondaryTextColor) },
                                placeholder = { Text("Any", color = placeholderColor) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = secondaryTextColor,
                                    unfocusedLabelColor = secondaryTextColor,
                                    focusedPlaceholderColor = placeholderColor,
                                    unfocusedPlaceholderColor = placeholderColor,
                                    cursorColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue
                                )
                            )
                        }
                    }
                }

                // Area Range
                FilterSection(title = "Area Range (sq.ft)", isDarkMode = isDarkMode) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (filters.minArea > 0) "Min: ${filters.minArea} sq.ft" else "No minimum",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                            Text(
                                if (filters.maxArea > 0) "Max: ${filters.maxArea} sq.ft" else "No maximum",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = if (filters.minArea > 0) filters.minArea.toString() else "",
                                onValueChange = {
                                    filters = filters.copy(minArea = it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Min Area", color = secondaryTextColor) },
                                placeholder = { Text("0", color = placeholderColor) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = secondaryTextColor,
                                    unfocusedLabelColor = secondaryTextColor,
                                    focusedPlaceholderColor = placeholderColor,
                                    unfocusedPlaceholderColor = placeholderColor,
                                    cursorColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue
                                )
                            )

                            OutlinedTextField(
                                value = if (filters.maxArea > 0) filters.maxArea.toString() else "",
                                onValueChange = {
                                    filters = filters.copy(maxArea = it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Max Area", color = secondaryTextColor) },
                                placeholder = { Text("Any", color = placeholderColor) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = secondaryTextColor,
                                    unfocusedLabelColor = secondaryTextColor,
                                    focusedPlaceholderColor = placeholderColor,
                                    unfocusedPlaceholderColor = placeholderColor,
                                    cursorColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue
                                )
                            )
                        }
                    }
                }

                // Bedrooms
                FilterSection(title = "Bedrooms", isDarkMode = isDarkMode) {
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
                                label = {
                                    Text(
                                        bed,
                                        color = if (filters.bedrooms == bed) chipLabelColor else chipLabelColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipContainerColor,
                                    selectedLabelColor = chipLabelColor,
                                    containerColor = chipContainerColor,
                                    labelColor = chipLabelColor
                                )
                            )
                        }
                    }
                }

                // Furnishing
                FilterSection(title = "Furnishing", isDarkMode = isDarkMode) {
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
                                label = {
                                    Text(
                                        furn,
                                        color = if (filters.furnishing == furn) chipLabelColor else chipLabelColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipContainerColor,
                                    selectedLabelColor = chipLabelColor,
                                    containerColor = chipContainerColor,
                                    labelColor = chipLabelColor
                                )
                            )
                        }
                    }
                }

                // Additional Features
                FilterSection(title = "Additional Features", isDarkMode = isDarkMode) {
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
                            Text("Parking Available", fontSize = 15.sp, color = textColor)
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
                                    checkedColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                    uncheckedColor = secondaryTextColor,
                                    checkmarkColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimary else Color.White
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
                                Text("Pets Allowed", fontSize = 15.sp, color = textColor)
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
                                        checkedColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue,
                                        uncheckedColor = secondaryTextColor,
                                        checkmarkColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Amenities
                FilterSection(title = "Amenities", isDarkMode = isDarkMode) {
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
                                label = {
                                    Text(
                                        amenity,
                                        fontSize = 13.sp,
                                        color = if (filters.amenities.contains(amenity)) chipLabelColor else chipLabelColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipContainerColor,
                                    selectedLabelColor = chipLabelColor,
                                    containerColor = chipContainerColor,
                                    labelColor = chipLabelColor
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
                color = backgroundColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeFilterCount = countActiveFilters(filters)
                    val buttonColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Blue

                    Button(
                        onClick = {
                            onFiltersApply(filters)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
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
    if (filters.minArea > 0 || filters.maxArea > 0) count++
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
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}