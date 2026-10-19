package com.example.gharbato.view

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoPalette

/** A compact, photo-free facts row shared by property cards across the app. */
@Composable
fun PropertyCardFacts(
    property: PropertyModel,
    modifier: Modifier = Modifier,
    tint: Color = GharBatoPalette.brand,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PropertyCardFact(Icons.Default.SquareFoot, property.sqft.ifBlank { "—" }, tint, contentColor, Modifier.weight(1f))
        PropertyCardFact(Icons.Default.SingleBed, "${property.bedrooms} beds", tint, contentColor, Modifier.weight(1f))
        PropertyCardFact(Icons.Default.Bathtub, "${property.bathrooms} baths", tint, contentColor, Modifier.weight(1f))
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
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            color = contentColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
