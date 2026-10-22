package com.example.gharbato.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.LocalGharBatoActionColors

@Composable
fun PurposeContentScreen(
    selectedPurpose: String,
    onPurposeChange: (String) -> Unit,
    selectedPropertyType: String,
    onPropertyTypeChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val actions = LocalGharBatoActionColors.current

    Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp)) {
        Text(
            text = "What would you like to do?",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(start = 5.dp, top = 20.dp, end = 5.dp, bottom = 20.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Sell", "Rent", "Book").forEachIndexed { index, purpose ->
                val selected = selectedPurpose == purpose
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onPurposeChange(purpose) }
                        ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) actions.container else colors.surface,
                        contentColor = if (selected) actions.onContainer else colors.onSurface
                    ),
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) colors.primary else colors.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                when (index) {
                                    0 -> R.drawable.tag
                                    1 -> R.drawable.home
                                    else -> R.drawable.calendar
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (selected) actions.onContainer else colors.onSurfaceVariant
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "Sell Property"
                                    1 -> "Rent Out Property"
                                    else -> "Short-Term Booking"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (index) {
                                    0 -> "List your property for sale"
                                    1 -> "Find tenants for long-term rental"
                                    else -> "Vacation rental or daily booking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) actions.onContainer else colors.onSurfaceVariant
                            )
                        }
                        Box(Modifier.size(24.dp)) {
                            if (selected) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_check_24),
                                    contentDescription = null,
                                    tint = actions.onContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Property Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            listOf("Apartment", "House", "Villa", "Studio").forEach { type ->
                val selected = selectedPropertyType == type
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onPropertyTypeChange(type) }
                        ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) actions.container else colors.surface,
                        contentColor = if (selected) actions.onContainer else colors.onSurface
                    ),
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) colors.primary else colors.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_check_24),
                                contentDescription = null,
                                tint = actions.onContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
