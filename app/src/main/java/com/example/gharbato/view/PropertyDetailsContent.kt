package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.gharbato.R
import com.example.gharbato.model.NearbyPlace
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoShapes
import com.example.gharbato.ui.theme.GharBatoSpacing
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import com.example.gharbato.utils.formatPropertyPrice
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/** Backend-free presentation: navigation, persistence, and communication stay in the activity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailsContent(
    property: PropertyModel,
    similarProperties: List<PropertyModel> = emptyList(),
    isLoadingSimilar: Boolean = false,
    nearbyPlaces: List<NearbyPlace> = emptyList(),
    isLoadingNearby: Boolean = false,
    onBack: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onOfferClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onSimilarPropertyClick: (PropertyModel) -> Unit = {},
    modifier: Modifier = Modifier,
    canContact: Boolean = property.ownerId.isNotBlank(),
    isOwnListing: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = MaterialTheme.colorScheme
    val available = !property.propertyStatus.equals("SOLD", true)
    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Property", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = property.isFavorite,
                        onCheckedChange = { onFavoriteClick() },
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = if (property.isFavorite) "Remove from saved" else "Save property"
                        }
                    ) {
                        Icon(if (property.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onShareClick, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.IosShare, contentDescription = "Share property", modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            PropertyContactBar(onMessageClick, onCallClick, canContact && available)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("property_detail_list"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(GharBatoSpacing.section)
        ) {
            item(key = "introduction") {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    PropertyDetailGallery(property, Modifier.padding(horizontal = GharBatoSpacing.screenHorizontal))
                    PropertyIntroduction(property, onMapClick)
                    PropertyFactsSummary(property)
                }
            }
            if (!property.description.isNullOrBlank()) {
                item(key = "about") {
                    DetailSection("About this property") {
                        var expanded by rememberSaveable(property.id) { mutableStateOf(false) }
                        var overflows by remember(property.id, property.description) { mutableStateOf(false) }
                        Text(property.description.orEmpty(), style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant, maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow })
                        if (overflows || expanded) {
                            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (expanded) "Show less" else "Read more")
                            }
                        }
                    }
                }
            }
            val details = buildList {
                property.floor.takeIf(String::isNotBlank)?.let { add("Floor" to it) }
                property.furnishing.takeIf(String::isNotBlank)?.let { add("Furnishing" to it) }
                property.totalRooms.takeIf(String::isNotBlank)?.let { add("Total rooms" to it) }
                property.kitchen.takeIf(String::isNotBlank)?.let { add("Kitchen" to it) }
                add("Parking" to if (property.parking) "Available" else "Not available")
                add("Pets" to if (property.petsAllowed) "Allowed" else "Not allowed")
            }
            item(key = "details") {
                DetailSection("Property details") { PropertyGroupedRows(details) }
            }
            if (property.amenities.isNotEmpty()) {
                item(key = "amenities") {
                    DetailSection("Amenities") { PropertyAmenities(property.amenities) }
                }
            }
            val terms = buildList {
                property.availableFrom?.takeIf(String::isNotBlank)?.let { add("Available from" to it) }
                property.minimumLease?.takeIf(String::isNotBlank)?.let { add("Minimum lease" to it) }
                property.securityDeposit?.takeIf(String::isNotBlank)?.let { add("Security deposit" to it) }
                property.advancePayment?.takeIf(String::isNotBlank)?.let { add("Advance payment" to it) }
                property.utilitiesIncluded?.takeIf(String::isNotBlank)?.let { add("Utilities" to it) }
                property.commission?.takeIf(String::isNotBlank)?.let { add("Commission" to it) }
            }
            if (!property.marketType.equals("Sell", true) && terms.isNotEmpty()) {
                item(key = "terms") {
                    DetailSection("Rental terms") { PropertyGroupedRows(terms) }
                }
            }
            if (property.location.isNotBlank() || property.hasValidCoordinates) {
                item(key = "location") {
                    DetailSection("Location") {
                        Text(property.location, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                        if (property.hasValidCoordinates) {
                            PropertyLocationPreview(property, onMapClick)
                            TextButton(onClick = onMapClick, contentPadding = PaddingValues(0.dp)) {
                                Text("Explore the area")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            if (isLoadingNearby) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Finding nearby places", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                            } else if (nearbyPlaces.isNotEmpty()) {
                                PropertyNearbyPlaces(nearbyPlaces)
                            }
                        }
                    }
                }
            }
            item(key = "owner") {
                DetailSection("Listed by") {
                    PropertyOwner(property, canContact, available, isOwnListing, onOfferClick)
                }
            }
            if (similarProperties.isNotEmpty() || isLoadingSimilar) {
                item(key = "similar") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Similar homes", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = GharBatoSpacing.screenHorizontal))
                        if (isLoadingSimilar && similarProperties.isEmpty()) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = GharBatoSpacing.screenHorizontal))
                        } else {
                            LazyRow(contentPadding = PaddingValues(horizontal = GharBatoSpacing.screenHorizontal),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(similarProperties, key = { it.id }) { similar ->
                                    PropertyListingCard(similar, { onSimilarPropertyClick(similar) }, Modifier.width(280.dp), compact = true)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "report") {
                Column(Modifier.padding(horizontal = GharBatoSpacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = colors.outlineVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Listing #${property.id}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = onReportClick) {
                            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Report listing", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyIntroduction(property: PropertyModel, onMapClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val purpose = when (property.marketType.lowercase()) { "rent" -> "For rent"; "sell" -> "For sale"; "book" -> "Short stay"; else -> "" }
    Column(Modifier.padding(horizontal = GharBatoSpacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listOf(property.propertyType, purpose).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.weight(1f))
            if (property.propertyStatus.equals("SOLD", true) || property.propertyStatus.equals("ON_HOLD", true)) {
                Surface(shape = GharBatoShapes.small, color = colors.surfaceVariant) {
                    Text(if (property.propertyStatus.equals("SOLD", true)) "Sold" else "On hold",
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
        Text(property.title.ifBlank { property.propertyType }, style = MaterialTheme.typography.headlineLarge, color = colors.onBackground)
        if (property.location.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = property.hasValidCoordinates, onClick = onMapClick)
                    .heightIn(min = 48.dp).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Text(property.location, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (property.hasValidCoordinates) Icon(Icons.Filled.ChevronRight, contentDescription = "View location", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(formatPropertyPrice(property.price, property.marketType), style = MaterialTheme.typography.headlineSmall,
            color = colors.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PropertyFactsSummary(property: PropertyModel) {
    val isLand = property.propertyType.contains("land", true)
    val facts = buildList {
        if (!isLand) {
            add(Triple(Icons.Outlined.Bed, if (property.bedrooms == 0) "Studio" else property.bedrooms.toString(), if (property.bedrooms == 0) "Layout" else "Bedrooms"))
            add(Triple(Icons.Outlined.Bathtub, property.bathrooms.toString(), if (property.bathrooms == 1) "Bathroom" else "Bathrooms"))
        }
        if (property.sqft.isNotBlank()) {
            val area = property.sqft.replace(Regex("(?i)\\s*(sq\\.?\\s*ft\\.?|sqft)$"), "").trim()
            add(Triple(Icons.Outlined.SquareFoot, area, "sq.ft"))
        }
    }
    if (facts.isEmpty()) return
    Surface(color = MaterialTheme.colorScheme.surface, shape = GharBatoShapes.card,
        modifier = Modifier.fillMaxWidth().padding(horizontal = GharBatoSpacing.screenHorizontal)) {
        Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            facts.forEachIndexed { index, fact ->
                if (index > 0) VerticalDivider(Modifier.height(48.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(fact.first, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(fact.second, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(fact.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = GharBatoSpacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun PropertyGroupedRows(rows: List<Pair<String, String>>) {
    Surface(shape = GharBatoShapes.card, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertyAmenities(amenities: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp), maxItemsInEachRow = 2) {
        amenities.distinct().forEach { amenity ->
            Row(Modifier.weight(1f).widthIn(min = 130.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(amenitySymbol(amenity), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(amenity, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun amenitySymbol(name: String): ImageVector = when {
    name.contains("wifi", true) -> Icons.Outlined.Wifi
    name.contains("parking", true) -> Icons.Outlined.DirectionsCar
    name.contains("air conditioning", true) -> Icons.Outlined.AcUnit
    name.contains("security", true) -> Icons.Outlined.Shield
    name.contains("garden", true) -> Icons.Outlined.Yard
    name.contains("balcony", true) -> Icons.Outlined.Balcony
    name.contains("elevator", true) -> Icons.Outlined.Elevator
    name.contains("water", true) -> Icons.Outlined.WaterDrop
    name.contains("power", true) -> Icons.Outlined.Bolt
    name.contains("washing", true) -> Icons.Outlined.LocalLaundryService
    name.contains("refrigerator", true) -> Icons.Outlined.Kitchen
    name.contains("pool", true) -> Icons.Outlined.Pool
    name.contains("gym", true) -> Icons.Outlined.FitnessCenter
    else -> Icons.Outlined.Check
}

@Composable
private fun PropertyOwner(property: PropertyModel, canContact: Boolean, available: Boolean, isOwnListing: Boolean, onOfferClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val owner = property.ownerName.ifBlank { "Property owner" }
    Surface(shape = GharBatoShapes.card, color = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SubcomposeAsyncImage(
                    model = property.ownerImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    loading = { OwnerInitials(owner) }, error = { OwnerInitials(owner) }
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(owner, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(when {
                        isOwnListing -> "Your listing"
                        !available -> "This property has been sold"
                        !canContact -> "Contact details unavailable"
                        else -> "Property owner"
                    },
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            if (canContact && available) {
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth().clickable(onClick = onOfferClick).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
                    Text("Make an offer", style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun OwnerInitials(name: String) {
    val initials = name.split(' ').filter(String::isNotBlank).take(2).joinToString("") { it.take(1).uppercase() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun PropertyContactBar(onMessageClick: () -> Unit, onCallClick: () -> Unit, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Column {
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.65f))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCallClick, enabled = enabled, shape = GharBatoShapes.control,
                    modifier = Modifier.weight(0.8f).heightIn(min = 52.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    colors = GharBatoButtonDefaults.outlinedColors(),
                    border = GharBatoButtonDefaults.outlinedBorder(enabled)) {
                    Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Call", style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = onMessageClick, enabled = enabled, shape = GharBatoShapes.control,
                    modifier = Modifier.weight(1.3f).heightIn(min = 52.dp),
                    colors = GharBatoButtonDefaults.primaryColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Message owner", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PropertyDetailGallery(property: PropertyModel, modifier: Modifier = Modifier) {
    val photos = remember(property.images) { (listOf(property.imageUrl) + property.images.values.flatten()).filter(String::isNotBlank).distinct() }
    var showFullscreen by rememberSaveable(property.id) { mutableStateOf(false) }
    val pager = rememberPagerState(pageCount = { photos.size })
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val gallerySize = if (photos.isEmpty()) Modifier.heightIn(min = 176.dp) else Modifier.aspectRatio(1.27f)
    Box(modifier.fillMaxWidth().then(gallerySize).clip(GharBatoShapes.large).background(colors.surfaceVariant)) {
        if (photos.isEmpty()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Home, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                Text("No photos available", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        } else {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().testTag("property_detail_gallery")) { page ->
                SubcomposeAsyncImage(model = photos[page], contentDescription = "Property photo ${page + 1}",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clickable { showFullscreen = true },
                    loading = { PropertyPhotoPlaceholder() }, error = { PropertyPhotoPlaceholder() })
            }
            Surface(onClick = { showFullscreen = true }, color = colors.scrim.copy(alpha = 0.6f), contentColor = Color.White,
                shape = GharBatoShapes.small, modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "View all photos", modifier = Modifier.size(16.dp))
                    Text("${pager.currentPage + 1} / ${photos.size}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    if (showFullscreen && photos.isNotEmpty()) {
        val fullscreenPager = rememberPagerState(initialPage = pager.currentPage, pageCount = { photos.size })
        val closeGallery = {
            showFullscreen = false
            scope.launch { pager.scrollToPage(fullscreenPager.currentPage) }
            Unit
        }
        Dialog(onDismissRequest = closeGallery, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                Column(Modifier.systemBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = closeGallery) { Icon(Icons.Filled.Close, contentDescription = "Close gallery") }
                        Text("${fullscreenPager.currentPage + 1} / ${photos.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    HorizontalPager(state = fullscreenPager, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                        SubcomposeAsyncImage(photos[page], contentDescription = "Property photo ${page + 1}", contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(), loading = { PropertyPhotoPlaceholder() }, error = { PropertyPhotoPlaceholder() })
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyLocationPreview(property: PropertyModel, onMapClick: () -> Unit) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(property.latLng, 15f) }
    Box(Modifier.fillMaxWidth().height(180.dp).clip(GharBatoShapes.card)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
            properties = MapProperties(mapStyleOptions = if (dark) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false,
                rotationGesturesEnabled = false, tiltGesturesEnabled = false, mapToolbarEnabled = false),
            onMapClick = { onMapClick() }
        ) {
            Marker(state = MarkerState(property.latLng), title = property.title,
                icon = BitmapDescriptorFactory.defaultMarker(278f))
        }
    }
}

@Composable
private fun PropertyNearbyPlaces(nearby: List<NearbyPlace>) {
    val realPlaces = nearby.filterNot { it.id.startsWith("fallback_") }.sortedBy { it.distance }.take(3)
    if (realPlaces.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("In the neighbourhood", style = MaterialTheme.typography.titleSmall)
        realPlaces.forEach { place ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(place.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(place.formattedDistance, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Nearby places from OpenStreetMap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
