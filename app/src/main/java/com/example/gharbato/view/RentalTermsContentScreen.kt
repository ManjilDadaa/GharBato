package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.Blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalTermsContentScreen(
    state: PropertyListingState,
    onStateChange: (PropertyListingState) -> Unit
) {
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            "Rental Terms",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode)
                MaterialTheme.colorScheme.onBackground
            else
                Color(0xFF2C2C2C)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Define terms for your rental property",
            fontSize = 14.sp,
            color = if (isDarkMode)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                Color(0xFF999999)
        )

        Spacer(Modifier.height(24.dp))

        TermDropdownField(
            "Utilities",
            state.utilitiesIncluded,
            listOf(
                "Included (electricity extra)",
                "Included (all utilities)",
                "Not included",
                "Partially included"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(utilitiesIncluded = it)) }

        TermDropdownField(
            "Commission",
            state.commission,
            listOf(
                "No commission",
                "1 month rent",
                "Half month rent",
                "Negotiable"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(commission = it)) }

        TermDropdownField(
            "Advance Payment",
            state.advancePayment,
            listOf(
                "1 month rent",
                "2 months rent",
                "3 months rent",
                "Negotiable"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(advancePayment = it)) }

        TermDropdownField(
            "Security Deposit",
            state.securityDeposit,
            listOf(
                "1 month rent",
                "2 months rent",
                "3 months rent",
                "Negotiable"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(securityDeposit = it)) }

        TermDropdownField(
            "Minimum Lease",
            state.minimumLease,
            listOf(
                "6 months",
                "12 months",
                "24 months",
                "Flexible"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(minimumLease = it)) }

        TermDropdownField(
            "Available From",
            state.availableFrom,
            listOf(
                "Immediate",
                "1 week",
                "2 weeks",
                "1 month",
                "2 months"
            ),
            isDarkMode = isDarkMode
        ) { onStateChange(state.copy(availableFrom = it)) }

        Spacer(Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode)
                    Color(0xFF1A2F3A)
                else
                    Color(0xFFF0F9FF)
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
                    tint = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "These terms help tenants understand rental conditions upfront and can be negotiated later.",
                    fontSize = 13.sp,
                    color = if (isDarkMode)
                        MaterialTheme.colorScheme.onSurface
                    else
                        Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermDropdownField(
    label: String,
    value: String,
    options: List<String>,
    isDarkMode: Boolean,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(bottom = 16.dp)) {

        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode)
                MaterialTheme.colorScheme.onSurface
            else
                Color.DarkGray
        )

        Spacer(Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDarkMode) Color(0xFF82B1FF) else Blue,
                    unfocusedBorderColor = if (isDarkMode)
                        MaterialTheme.colorScheme.outline
                    else
                        Color(0xFF999999).copy(0.5f),
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
                ),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(
                        color = if (isDarkMode)
                            MaterialTheme.colorScheme.surface
                        else
                            Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                options.forEach { option ->
                    val isSelected = value == option

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    if (isDarkMode) Color(0xFF82B1FF) else Blue
                                } else {
                                    if (isDarkMode)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        Color.DarkGray
                                }
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    if (isDarkMode)
                                        Color(0xFF1A2F3A)
                                    else
                                        Blue.copy(alpha = 0.08f)
                                } else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp)
                    )
                }
            }

        }
    }
}