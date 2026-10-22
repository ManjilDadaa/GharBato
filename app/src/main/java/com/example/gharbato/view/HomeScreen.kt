package com.example.gharbato.view

import com.example.gharbato.ui.theme.LocalGharBatoActionColors
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoDimensions
import com.example.gharbato.ui.theme.GharBatoShapes
import com.example.gharbato.ui.theme.GharBatoSpacing
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.example.gharbato.viewmodel.UserViewModelProvider
import kotlin.math.ln

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(LocalContext.current)
    ),
    onNavigateToSearch: () -> Unit = {}
) {
    val context = LocalContext.current

    // Get UserViewModel for notifications
    val userViewModel = remember { UserViewModelProvider.getInstance() }
    val unreadCount by userViewModel.unreadCount.observeAsState(0)

    // Get UI state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val allProperties = uiState.allLoadedProperties
    var selectedMarketType by rememberSaveable { mutableStateOf("") }
    val homeProperties = remember(allProperties, selectedMarketType) {
        allProperties.filter { property ->
            selectedMarketType.isBlank() ||
                property.marketType.trim().equals(selectedMarketType, ignoreCase = true)
        }
    }
    val seeAllProperties = {
        viewModel.resetFilters()
        viewModel.applyFilters(PropertyFilters(marketType = selectedMarketType))
        onNavigateToSearch()
    }

    // Calculate featured properties using professional algorithm
    val featuredProperties = remember(homeProperties) {
        calculateFeaturedProperties(homeProperties)
    }

    // Get recent properties (last 2 weeks, sorted by date)
    val recentProperties = remember(homeProperties) {
        val twoWeeksAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000)
        homeProperties
            .filter { it.updatedAt >= twoWeeksAgo }
            .sortedByDescending { it.updatedAt }
            .take(20)
    }

    // Popular properties (high views)
    val popularProperties = remember(homeProperties) {
        homeProperties
            .sortedByDescending { it.totalViews }
            .take(10)
    }

    // Get MaterialTheme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Start observing notifications
    LaunchedEffect(Unit) {
        userViewModel.startObservingNotifications()
    }

    Scaffold(
        containerColor = backgroundColor,
        floatingActionButton = {
            AIAssistanceFAB(
                onClick = {
                    val intent = Intent(context, GeminiChatActivity::class.java)
                    context.startActivity(intent)
                },
                backgroundColor = MaterialTheme.colorScheme.primary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(top = padding.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Header Section
                item {
                    HomeHeader(
                        unreadCount = unreadCount,
                        onNotificationClick = {
                            val intent = Intent(context, NotificationActivity::class.java)
                            context.startActivity(intent)
                        },
                        backgroundColor = backgroundColor,
                        onBackgroundColor = onBackgroundColor
                    )
                }

                item {
                    HomeSearchSection(
                        onSearch = { query ->
                            viewModel.resetFilters()
                            viewModel.updateSearchQuery(query)
                            viewModel.performSearch()
                            onNavigateToSearch()
                        }
                    )
                }

                item {
                    QuickActionsSection(
                        selectedMarketType = selectedMarketType,
                        onMarketTypeSelected = { selectedMarketType = it }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Loading properties...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Featured Properties (Algorithm-based)
                if (featuredProperties.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Featured properties",
                            subtitle = "",
                            icon = Icons.Default.Star,
                            showViewAll = true,
                            onViewAllClick = seeAllProperties,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }

                    item {
                        FeaturedPropertiesCarousel(
                            properties = featuredProperties,
                            onPropertyClick = { property ->
                                val intent = Intent(context, PropertyDetailActivity::class.java)
                                intent.putExtra("propertyId", property.id)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Recent Properties
                if (recentProperties.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Just listed",
                            subtitle = "",
                            icon = Icons.Default.NewReleases,
                            showViewAll = true,
                            onViewAllClick = seeAllProperties,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }

                    items(recentProperties.take(5)) { property ->
                        ModernPropertyCard(
                            property = property,
                            onClick = {
                                val intent = Intent(context, PropertyDetailActivity::class.java)
                                intent.putExtra("propertyId", property.id)
                                context.startActivity(intent)
                            },
                            surfaceColor = surfaceColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }
                }

                // Popular Properties
                if (popularProperties.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Popular right now",
                            subtitle = "",
                            icon = Icons.Default.Visibility,
                            showViewAll = false,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }

                    item {
                        PopularPropertiesRow(
                            properties = popularProperties.take(5),
                            onPropertyClick = { property ->
                                val intent = Intent(context, PropertyDetailActivity::class.java)
                                intent.putExtra("propertyId", property.id)
                                context.startActivity(intent)
                            },
                            surfaceColor = surfaceColor,
                            onSurfaceColor = onSurfaceColor
                        )
                    }
                }

                // Empty State
                if (homeProperties.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateSection(
                            onSurfaceVariantColor = onSurfaceVariantColor,
                            filtered = selectedMarketType.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIAssistanceFAB(
    onClick: () -> Unit,
    backgroundColor: Color
) {
    val actions = LocalGharBatoActionColors.current
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        containerColor = actions.container,
        contentColor = actions.onContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 2.dp,
            pressedElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Assistant",
                tint = actions.onContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Professional Featured Properties Algorithm
 *
 * Scoring Factors:
 * 1. Engagement Score (40%) - Views, unique viewers, recency
 * 2. Quality Score (30%) - Image count, description length, completeness
 * 3. Freshness Score (20%) - How recently updated
 * 4. Activity Score (10%) - Recent views trend
 *
 * Returns top 8 properties with highest scores
 */
fun calculateFeaturedProperties(properties: List<PropertyModel>): List<PropertyModel> {
    if (properties.isEmpty()) return emptyList()

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val oneWeekMs = 7 * oneDayMs

    // Calculate max values for normalization
    val maxViews = properties.maxOfOrNull { it.totalViews }?.toFloat() ?: 1f
    val maxUniqueViewers = properties.maxOfOrNull { it.uniqueViewers }?.toFloat() ?: 1f

    val scoredProperties = properties.map { property ->
        // 1. ENGAGEMENT SCORE (40%)
        val viewScore = (property.totalViews / maxViews) * 0.5f
        val uniqueViewerScore = (property.uniqueViewers / maxUniqueViewers) * 0.5f
        val engagementScore = (viewScore + uniqueViewerScore) * 0.4f

        // 2. QUALITY SCORE (30%)
        val imageCount = property.images.values.flatten().size
        val imageScore = (imageCount.coerceAtMost(10) / 10f) * 0.4f

        val descriptionScore = property.description?.let { desc ->
            (desc.length.coerceAtMost(500) / 500f) * 0.3f
        } ?: 0f

        val completenessScore = calculateCompletenessScore(property) * 0.3f
        val qualityScore = (imageScore + descriptionScore + completenessScore) * 0.3f

        // 3. FRESHNESS SCORE (20%)
        val daysSinceUpdate = ((now - property.updatedAt) / oneDayMs).toFloat()
        val freshnessScore = when {
            daysSinceUpdate <= 1 -> 1.0f      // Today
            daysSinceUpdate <= 3 -> 0.8f      // Last 3 days
            daysSinceUpdate <= 7 -> 0.6f      // Last week
            daysSinceUpdate <= 14 -> 0.4f     // Last 2 weeks
            daysSinceUpdate <= 30 -> 0.2f     // Last month
            else -> 0.1f                       // Older
        } * 0.2f

        // 4. ACTIVITY SCORE (10%) - Logarithmic view score
        val activityScore = if (property.totalViews > 0) {
            (ln(property.totalViews.toFloat() + 1) / ln(maxViews + 1)) * 0.1f
        } else 0f

        // Calculate total score
        val totalScore = engagementScore + qualityScore + freshnessScore + activityScore

        Pair(property, totalScore)
    }

    // Return top 8 properties
    return scoredProperties
        .sortedByDescending { it.second }
        .take(8)
        .map { it.first }
}

/**
 * Calculate property completeness score (0.0 to 1.0)
 */
fun calculateCompletenessScore(property: PropertyModel): Float {
    var score = 0f
    var totalFields = 0f

    // Essential fields
    if (property.title.isNotBlank()) score += 1f
    totalFields += 1f

    if (property.developer.isNotBlank()) score += 1f
    totalFields += 1f

    if (property.location.isNotBlank()) score += 1f
    totalFields += 1f

    if (property.price.isNotBlank()) score += 1f
    totalFields += 1f

    if (property.images.isNotEmpty()) score += 1f
    totalFields += 1f

    // Optional but valuable fields
    if (!property.description.isNullOrBlank()) score += 0.5f
    totalFields += 0.5f

    if (property.amenities.isNotEmpty()) score += 0.5f
    totalFields += 0.5f

    if (property.ownerImageUrl.isNotBlank()) score += 0.25f
    totalFields += 0.25f

    return if (totalFields > 0) score / totalFields else 0f
}

@Composable
fun PopularPropertiesRow(
    properties: List<PropertyModel>,
    onPropertyClick: (PropertyModel) -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = GharBatoSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.medium)
    ) {
        items(properties) { property ->
            CompactPropertyCard(
                property = property,
                onClick = { onPropertyClick(property) },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )
        }
    }
}

@Composable
fun CompactPropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    PropertyListingCard(
        property = property,
        onClick = onClick,
        compact = true,
        modifier = Modifier.width(264.dp)
    )
}

@Composable
internal fun HomeSearchSection(onSearch: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submit = { focusManager.clearFocus(); onSearch(query.trim()) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = GharBatoSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search city, area, or property", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { submit() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search properties", tint = MaterialTheme.colorScheme.primary)
                }
            },
            singleLine = true,
            shape = GharBatoShapes.control,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun HomeHeader(
    unreadCount: Int,
    onNotificationClick: () -> Unit,
    backgroundColor: Color,
    onBackgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.xLarge)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "GharBato",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Find your dream property",
                        style = MaterialTheme.typography.headlineLarge,
                        color = onBackgroundColor
                    )
                }

                // Notification Bell
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(GharBatoDimensions.touchTarget)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(GharBatoDimensions.standardIcon)
                        )
                    }

                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = LocalGharBatoActionColors.current.container,
                            contentColor = LocalGharBatoActionColors.current.onContainer
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 10.sp,
                                color = LocalGharBatoActionColors.current.onContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    selectedMarketType: String,
    onMarketTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GharBatoSpacing.screenHorizontal)
            .padding(vertical = GharBatoSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.small)
    ) {
        listOf(
            Triple("Rent", "Rent", Icons.Default.Key),
            Triple("Buy", "Sell", Icons.Default.Home),
            Triple("Stay", "Book", Icons.Default.Hotel)
        ).forEach { (title, marketType, icon) ->
            val selected = selectedMarketType == marketType
            FilterChip(
                selected = selected,
                onClick = { onMarketTypeSelected(if (selected) "" else marketType) },
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.small, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).testTag("home_filter_icon_$marketType")
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = GharBatoDimensions.touchTarget),
                shape = RoundedCornerShape(percent = 50)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    showViewAll: Boolean = false,
    onViewAllClick: () -> Unit = {},
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GharBatoSpacing.screenHorizontal)
            .padding(top = GharBatoSpacing.xLarge, bottom = GharBatoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = onBackgroundColor
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor
                    )
                }
            }
        }

        if (showViewAll) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVariantColor
                )
            }
        }
    }
}

@Composable
fun FeaturedPropertiesCarousel(
    properties: List<PropertyModel>,
    onPropertyClick: (PropertyModel) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = GharBatoSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.medium)
    ) {
        items(properties) { property ->
            FeaturedPropertyCard(
                property = property,
                onClick = { onPropertyClick(property) }
            )
        }
    }
}

@Composable
fun FeaturedPropertyCard(
    property: PropertyModel,
    onClick: () -> Unit
) {
    PropertyListingCard(
        property = property,
        onClick = onClick,
        compact = true,
        modifier = Modifier.width(296.dp)
    )
}

@Composable
fun PropertyFeature(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

@Composable
fun HomePropertyStatChip(
    icon: ImageVector,
    value: String,
    label: String?,
    iconTint: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.xSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp
                )
                if (label != null) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernPropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    PropertyListingCard(
        property = property,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.small)
    )
}

@Composable
fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EmptyStateSection(
    onSurfaceVariantColor: Color,
    filtered: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = onSurfaceVariantColor.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(GharBatoSpacing.medium))

        Text(
            text = if (filtered) "No properties in this category" else "No properties available",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(GharBatoSpacing.xSmall))

        Text(
            text = if (filtered) "Choose another filter or tap it again to see all properties" else "Check back later for new listings",
            fontSize = 13.sp,
            color = onSurfaceVariantColor.copy(alpha = 0.7f)
        )
    }
}
