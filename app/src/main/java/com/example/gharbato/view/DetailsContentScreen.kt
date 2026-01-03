package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@Composable
fun DetailsContentScreen(
    state: PropertyListingState,
    onStateChange: (PropertyListingState) -> Unit
) {

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
            .background(Color.White)
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Text(
            "Property Details",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0F9FF)
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
                        color = Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.selectedPurpose,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Property Type",
                        fontSize = 12.sp,
                        color = Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.selectedPropertyType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Developer/Owner Name
        CustomOutlinedTextField(
            value = state.developer,
            onValueChange = { onStateChange(state.copy(developer = it)) },
            label = "Owner/Developer Name",
            placeholder = "e.g., Ram Sharma",
            modifier = Modifier.fillMaxWidth()
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CustomOutlinedTextField(
                value = state.area,
                onValueChange = { onStateChange(state.copy(area = it)) },
                label = "Area (sq ft)",
                placeholder = "1200",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        CustomOutlinedTextField(
            value = state.location,
            onValueChange = { onStateChange(state.copy(location = it)) },
            label = "Location",
            placeholder = "e.g., Thamel, Kathmandu",
            modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.weight(1f)
            )

            // Furnishing Dropdown
            FurnishingDropdown(
                selectedFurnishing = state.furnishing,
                onFurnishingChange = { onStateChange(state.copy(furnishing = it)) },
                modifier = Modifier.weight(1f),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CustomOutlinedTextField(
                value = state.bedrooms,
                onValueChange = { onStateChange(state.copy(bedrooms = it)) },
                label = "Bedrooms",
                placeholder = "2",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CustomOutlinedTextField(
                value = state.kitchen,
                onValueChange = { onStateChange(state.copy(kitchen = it)) },
                label = "Kitchen",
                placeholder = "1",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Parking and Pets Allowed (Checkboxes)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9FAFB)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Additional Features",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Parking Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(parking = !state.parking)) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Parking Available", fontSize = 14.sp)
                    Checkbox(
                        checked = state.parking,
                        onCheckedChange = { onStateChange(state.copy(parking = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Blue,
                            uncheckedColor = Gray
                        )
                    )
                }

                //  Only show Pets Allowed for Rent or Book
                if (state.selectedPurpose == "Rent" || state.selectedPurpose == "Book") {
                    HorizontalDivider(color = Gray.copy(0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStateChange(state.copy(petsAllowed = !state.petsAllowed)) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pets Allowed", fontSize = 14.sp)
                        Checkbox(
                            checked = state.petsAllowed,
                            onCheckedChange = { onStateChange(state.copy(petsAllowed = it)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Blue,
                                uncheckedColor = Gray
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
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnishingDropdown(
    selectedFurnishing: String,
    onFurnishingChange: (String) -> Unit,
    modifier: Modifier = Modifier
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
            label = { Text("Furnishing") },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue,
                unfocusedBorderColor = Gray.copy(0.5f),
                focusedLabelColor = Blue,
                unfocusedLabelColor = Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            furnishingOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (selectedFurnishing == option) Blue else Color.Black,
                            fontWeight = if (selectedFurnishing == option) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onFurnishingChange(option)
                        expanded = false
                    }
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
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Blue,
            unfocusedBorderColor = Gray.copy(0.5f),
            focusedLabelColor = Blue,
            unfocusedLabelColor = Gray,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        maxLines = maxLines,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions
    )
}