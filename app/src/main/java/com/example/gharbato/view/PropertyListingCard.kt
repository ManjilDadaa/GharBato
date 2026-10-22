package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.ui.theme.GharBatoShapes
import com.example.gharbato.utils.formatPropertyPrice
import com.example.gharbato.utils.formatPropertyLocationSummary

/** The same information order and actions are used in discovery, search, and saved listings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListingCard(
    property: PropertyModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onShareClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onHideClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var showMenu by remember(property.id) { mutableStateOf(false) }
    val title = property.title.ifBlank { property.propertyType.ifBlank { "Property" } }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().clip(GharBatoShapes.card).combinedClickable(
            enabled = enabled,
            role = Role.Button,
            onClickLabel = "View property details",
            onLongClickLabel = "Property options",
            onLongClick = { showMenu = true },
            onClick = onClick
        ),
        shape = GharBatoShapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, colors.outlineVariant)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (compact) 1.55f else 1.6f)) {
            SubcomposeAsyncImage(
                model = property.imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { PropertyPhotoPlaceholder() },
                error = { PropertyPhotoPlaceholder() }
            )
            if (!compact || property.propertyStatus == "SOLD") {
                Surface(
                    color = colors.surface.copy(alpha = 0.96f),
                    contentColor = colors.onSurface,
                    shape = GharBatoShapes.small,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                ) {
                    Text(
                        text = if (property.propertyStatus == "SOLD") "Sold" else when (property.marketType.lowercase()) {
                            "sell" -> "For sale"
                            "rent" -> "For rent"
                            "book" -> "Short stay"
                            else -> property.propertyType
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                DropdownMenu(expanded = showMenu && enabled, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Share property") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            if (onShareClick != null) onShareClick() else {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "$title\n${formatPropertyPrice(property.price, property.marketType)}\n${property.location}\n\nFind this property on GharBato.")
                                }
                                context.startActivity(Intent.createChooser(share, "Share property"))
                            }
                        }
                    )
                    if (onHideClick != null) {
                        DropdownMenuItem(
                            text = { Text("Not interested") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = { showMenu = false; onHideClick() }
                        )
                    }
                    if (onReportClick != null) {
                        DropdownMenuItem(
                            text = { Text("Report listing") },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                            onClick = { showMenu = false; onReportClick() }
                        )
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                formatPropertyPrice(property.price, property.marketType),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatPropertyLocationSummary(property.location),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                thickness = 0.5.dp,
                color = colors.outlineVariant
            )
            PropertyCardFacts(property = property, tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PropertyPhotoPlaceholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PropertyListingCardPreview() {
    GharBatoTheme {
        PropertyListingCard(
            property = PropertyModel(title = "Bright two-bedroom apartment", price = "25000", marketType = "Rent", location = "Bhaisepati, Lalitpur", bedrooms = 2, bathrooms = 1, sqft = "1,100 sq.ft"),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
