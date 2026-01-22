package com.example.gharbato.view

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.Blue

@Composable
fun DetailsContentScreen(
    state: PropertyListingState,
    onStateChange: (PropertyListingState) -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    // Launcher for Map Location Picker
    val mapLocationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val latitude = data?.getDoubleExtra(MapLocationPickerActivity.RESULT_LATITUDE, 27.7172) ?: 27.7172
            val longitude = data?.getDoubleExtra(MapLocationPickerActivity.RESULT_LONGITUDE, 85.3240) ?: 85.3240
            val address = data?.getStringExtra(MapLocationPickerActivity.RESULT_ADDRESS) ?: ""

            onStateChange(
                state.copy(
                    latitude = latitude,
                    longitude = longitude,
                    location = address,
                    hasSelectedLocation = true
                )
            )
        }
    }

    val priceLabel = when(state.selectedPurpose) {
        "Sell" -> "Asking Price"
        "Rent" -> "Monthly Rent"
        "Book" -> "Nightly Rate"
        else -> "Price"
    }

    val pricePlaceholder = when(state.selectedPurpose) {
        "Sell" -> "5000000"
        "Rent" -> "25000"
        "Book" -> "5000"
        else -> "0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) MaterialTheme.colorScheme.background else Color.White)
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Text(
            "Property Details",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color(0xFF2C2C2C)
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    Color(0xFFF0F9FF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Purpose",
                        fontSize = 12.sp,
                        color = if (isDarkMode)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            Color(0xFF999999),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.selectedPurpose,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode)
                            Color(0xFF82B1FF)
                        else
                            Blue
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Property Type",
                        fontSize = 12.sp,
                        color = if (isDarkMode)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            Color(0xFF999999),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.selectedPropertyType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode)
                            Color(0xFF82B1FF)
                        else
                            Blue
                    )
                }
            }
        }

        // Property Title
        CustomOutlinedTextField(
            value = state.title,
            onValueChange = { onStateChange(state.copy(title = it)) },
            label = "Property Title",
            placeholder = "e.g., Modern 2BHK ${state.selectedPropertyType}",
            modifier = Modifier.fillMaxWidth(),
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Developer/Owner Name
        CustomOutlinedTextField(
            value = state.developer,
            onValueChange = { onStateChange(state.copy(developer = it)) },
            label = "Owner/Developer Name",
            placeholder = "e.g., Ram Sharma",
            modifier = Modifier.fillMaxWidth(),
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Price and Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = state.price,
                onValueChange = { onStateChange(state.copy(price = it)) },
                label = "$priceLabel (रु)",
                placeholder = pricePlaceholder,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )

            CustomOutlinedTextField(
                value = state.area,
                onValueChange = { onStateChange(state.copy(area = it)) },
                label = "Area (sq ft)",
                placeholder = "1200",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location with Map Picker
        LocationPickerField(
            location = state.location,
            hasSelectedLocation = state.hasSelectedLocation,
            onPickLocation = {
                val intent = android.content.Intent(context, MapLocationPickerActivity::class.java)
                mapLocationPickerLauncher.launch(intent)
            },
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Floor and Furnishing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = state.floor,
                onValueChange = { onStateChange(state.copy(floor = it)) },
                label = "Floor",
                placeholder = "e.g., 5 floors",
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode
            )

            // Furnishing Dropdown
            FurnishingDropdown(
                selectedFurnishing = state.furnishing,
                onFurnishingChange = { onStateChange(state.copy(furnishing = it)) },
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Rooms and Bedrooms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = state.totalRooms,
                onValueChange = { onStateChange(state.copy(totalRooms = it)) },
                label = "Total Rooms",
                placeholder = "10",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )

            CustomOutlinedTextField(
                value = state.bedrooms,
                onValueChange = { onStateChange(state.copy(bedrooms = it)) },
                label = "Bedrooms",
                placeholder = "2",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bathrooms and Kitchen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = state.bathrooms,
                onValueChange = { onStateChange(state.copy(bathrooms = it)) },
                label = "Bathrooms",
                placeholder = "2",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )

            CustomOutlinedTextField(
                value = state.kitchen,
                onValueChange = { onStateChange(state.copy(kitchen = it)) },
                label = "Kitchen",
                placeholder = "1",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Parking and Pets Allowed (Checkboxes)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode)
                    MaterialTheme.colorScheme.surface
                else
                    Color(0xFFF9FAFB)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Additional Features",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isDarkMode)
                        MaterialTheme.colorScheme.onSurface
                    else
                        Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Parking Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(parking = !state.parking)) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Parking Available",
                        fontSize = 14.sp,
                        color = if (isDarkMode)
                            MaterialTheme.colorScheme.onSurface
                        else
                            Color(0xFF2C2C2C)
                    )
                    Checkbox(
                        checked = state.parking,
                        onCheckedChange = { onStateChange(state.copy(parking = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                            uncheckedColor = if (isDarkMode)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                Color(0xFF999999)
                        )
                    )
                }

                // Only show Pets Allowed for Rent or Book
                if (state.selectedPurpose == "Rent" || state.selectedPurpose == "Book") {
                    HorizontalDivider(
                        color = if (isDarkMode)
                            MaterialTheme.colorScheme.outline
                        else
                            Color(0xFF999999).copy(0.3f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStateChange(state.copy(petsAllowed = !state.petsAllowed)) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pets Allowed",
                            fontSize = 14.sp,
                            color = if (isDarkMode)
                                MaterialTheme.colorScheme.onSurface
                            else
                                Color(0xFF2C2C2C)
                        )
                        Checkbox(
                            checked = state.petsAllowed,
                            onCheckedChange = { onStateChange(state.copy(petsAllowed = it)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                                uncheckedColor = if (isDarkMode)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    Color(0xFF999999)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        CustomOutlinedTextField(
            value = state.description,
            onValueChange = { onStateChange(state.copy(description = it)) },
            label = "Description",
            placeholder = "Describe your ${state.selectedPropertyType}...",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            singleLine = false,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LocationPickerField(
    location: String,
    hasSelectedLocation: Boolean,
    onPickLocation: () -> Unit,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickLocation),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSelectedLocation) {
                if (isDarkMode) Color(0xFF1B3A1F) else Color(0xFFE8F5E9)
            } else {
                if (isDarkMode) MaterialTheme.colorScheme.surface else Color(0xFFF5F5F5)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (hasSelectedLocation) {
                if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF4CAF50)
            } else {
                if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFF999999).copy(0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasSelectedLocation) Icons.Default.LocationOn else Icons.Default.AddLocation,
                contentDescription = "Location",
                tint = if (hasSelectedLocation) Color(0xFF4CAF50) else {
                    if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF999999)
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasSelectedLocation) "Location Selected" else "Select Property Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasSelectedLocation) {
                        Color(0xFF4CAF50)
                    } else {
                        if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (location.isNotEmpty()) location else "Tap to pin exact location on map",
                    fontSize = 13.sp,
                    color = if (isDarkMode)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        Color(0xFF999999),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Map",
                tint = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    Color(0xFF999999),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Show location info if selected
    if (hasSelectedLocation) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode)
                    Color(0xFF1A2F3A)
                else
                    Color(0xFFF0F9FF)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exact coordinates saved - Your property will appear at this location on the map",
                    fontSize = 12.sp,
                    color = if (isDarkMode)
                        MaterialTheme.colorScheme.onSurface
                    else
                        Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnishingDropdown(
    selectedFurnishing: String,
    onFurnishingChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val furnishingOptions = listOf("Fully Furnished", "Semi Furnished", "Unfurnished")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFurnishing,
            onValueChange = {},
            label = {
                Text(
                    "Furnishing",
                    color = if (expanded) {
                        if (isDarkMode) Color(0xFF82B1FF) else Blue
                    } else {
                        if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF999999)
                    }
                )
            },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                unfocusedBorderColor = if (isDarkMode)
                    MaterialTheme.colorScheme.outline
                else
                    Color(0xFF999999).copy(0.5f),
                focusedLabelColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                unfocusedLabelColor = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    Color(0xFF999999),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color(0xFF2C2C2C),
                unfocusedTextColor = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color(0xFF2C2C2C)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    color = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            furnishingOptions.forEach { option ->
                val isSelected = selectedFurnishing == option

                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                if (isDarkMode) Color(0xFF82B1FF) else Blue
                            } else {
                                if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.DarkGray
                            }
                        )
                    },
                    onClick = {
                        onFurnishingChange(option)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                if (isDarkMode) Color(0xFF1A2F3A) else Blue.copy(alpha = 0.08f)
                            } else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    isDarkMode: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = if (value.isNotEmpty()) {
                    if (isDarkMode) Color(0xFF82B1FF) else Blue
                } else {
                    if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF999999)
                }
            )
        },
        placeholder = {
            Text(
                placeholder,
                color = if (isDarkMode)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else
                    Color(0xFF999999).copy(alpha = 0.6f)
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
            unfocusedBorderColor = if (isDarkMode)
                MaterialTheme.colorScheme.outline
            else
                Color(0xFF999999).copy(0.5f),
            focusedLabelColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
            unfocusedLabelColor = if (isDarkMode)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                Color(0xFF999999),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = if (isDarkMode)
                MaterialTheme.colorScheme.onSurface
            else
                Color(0xFF2C2C2C),
            unfocusedTextColor = if (isDarkMode)
                MaterialTheme.colorScheme.onSurface
            else
                Color(0xFF2C2C2C),
            cursorColor = if (isDarkMode) Color(0xFF82B1FF) else Blue
        ),
        maxLines = maxLines,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions
    )
}