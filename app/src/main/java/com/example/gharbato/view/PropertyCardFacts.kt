package com.example.gharbato.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.SingleBed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gharbato.model.PropertyModel

/** A compact, photo-free facts row shared by property cards across the app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyCardFacts(
    property: PropertyModel,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PropertyCardFact(Icons.Default.SingleBed, if (property.bedrooms == 0) "Studio" else "${property.bedrooms} ${if (property.bedrooms == 1) "bed" else "beds"}", tint, contentColor)
        PropertyCardFact(Icons.Default.Bathtub, "${property.bathrooms} ${if (property.bathrooms == 1) "bath" else "baths"}", tint, contentColor)
        if (property.sqft.isNotBlank()) PropertyCardFact(Icons.Default.SquareFoot, property.sqft, tint, contentColor)
    }
}

@Composable
private fun PropertyCardFact(
    icon: ImageVector,
    value: String,
    tint: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
