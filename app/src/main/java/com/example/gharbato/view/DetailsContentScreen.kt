package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@Composable
fun DetailsContentScreen(purpose: String, propertyType: String) {

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("") }
    var bathrooms by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var kitchen by remember { mutableStateOf("") }
    var totalRooms by remember { mutableStateOf("") }

    val priceLabel = when(purpose) {
        "Sell" -> "Asking Price"
        "Rent" -> "Monthly Rent"
        "Book" -> "Nightly Rate"
        else -> "Price"
    }

    val pricePlaceholder = when(purpose) {
        "Sell" -> "250000"
        "Rent" -> "1200"
        "Book" -> "500"
        else -> "0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(2.dp)
    ) {
        Text(
            "Property Details",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(start = 2.dp,bottom = 16.dp)
        )
        // Show selected purpose and property type
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
                        purpose,
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
                        propertyType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue
                    )
                }
            }
        }

        // Property Title
        CustomOutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = "Property Title",
            placeholder = "e.g., Modern 2BHK $propertyType",
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        // Price and Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = "$priceLabel (रु)",
                placeholder = pricePlaceholder,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                maxLines = 1,
                singleLine = true
            )

            CustomOutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = "Area (sq ft)",
                placeholder = "e.g,1200",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                maxLines = 1,
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        CustomOutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = "Location",
            placeholder = "e.g,Thamel, Kathmandu",
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Total Rooms and Bed Rooms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location
            CustomOutlinedTextField(
                value = totalRooms,
                onValueChange = { totalRooms = it },
                label = "Total Rooms",
                placeholder = "e.g,10",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CustomOutlinedTextField(
                value = bedrooms,
                onValueChange = { bedrooms = it },
                label = "Bedrooms",
                placeholder = "e.g,2",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomOutlinedTextField(
                value = bathrooms,
                onValueChange = { bathrooms = it },
                label = "Bathrooms",
                placeholder = "e.g,2",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CustomOutlinedTextField(
                value = kitchen,
                onValueChange = { kitchen = it },
                label = "Kitchen",
                placeholder = "e.g,1",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        CustomOutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            placeholder = "Describe your $propertyType...",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(15.dp))
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
    maxLines: Int,
    singleLine: Boolean = true
){
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        label = { Text(label) },
        placeholder = { Text(placeholder)},
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Blue,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedLabelColor = Blue
        ),
        maxLines = maxLines,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions
    )
}