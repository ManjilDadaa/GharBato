package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.R
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoDimensions
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
    val activity = context as Activity

    // Get UserViewModel for notifications
    val userViewModel = remember { UserViewModelProvider.getInstance() }
    val unreadCount by userViewModel.unreadCount.observeAsState(0)

    // Get UI state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // IMPORTANT: Use ALL loaded properties, NOT filtered ones
    val allProperties = uiState.allLoadedProperties

    // Calculate featured properties using professional algorithm
    val featuredProperties = remember(allProperties) {
        calculateFeaturedProperties(allProperties)
    }

    // Get recent properties (last 2 weeks, sorted by date)
    val recentProperties = remember(allProperties) {
        val twoWeeksAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000)
        allProperties
            .filter { it.updatedAt >= twoWeeksAgo }
            .sortedByDescending { it.updatedAt }
            .take(20)
    }

    // Popular properties (high views)
    val popularProperties = remember(allProperties) {
        allProperties
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
            if (uiState.isLoading) {
                // Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading properties...",
                            fontSize = 14.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
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

                    // Quick Actions - Navigate to Search with pre-applied filter
                    item {
                        QuickActionsSection(
                            onRentClick = {
                                viewModel.applyFilters(PropertyFilters(marketType = "Rent"))
                                onNavigateToSearch()
                            },
                            onBuyClick = {
                                viewModel.applyFilters(PropertyFilters(marketType = "Sell"))
                                onNavigateToSearch()
                            },
                            onBookClick = {
                                viewModel.applyFilters(PropertyFilters(marketType = "Book"))
                                onNavigateToSearch()
                            }
                        )
                    }

                    // Featured Properties (Algorithm-based)
                    if (featuredProperties.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Featured Properties",
                                subtitle = "",
                                icon = Icons.Default.Star,
                                showViewAll = true,
                                onViewAllClick = {
                                    viewModel.resetFilters()
                                    onNavigateToSearch()
                                },
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
                                },
                                onFavoriteClick = { property ->
                                    viewModel.toggleFavorite(property)
                                }
                            )
                        }
                    }

                    // Recent Properties
                    if (recentProperties.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "New Listings",
                                subtitle = "",
                                icon = Icons.Default.NewReleases,
                                showViewAll = true,
                                onViewAllClick = onNavigateToSearch,
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
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(property)
                                },
                                surfaceColor = surfaceColor,
                                onSurfaceColor = onSurfaceColor,
                                onSurfaceVariantColor = onSurfaceVariantColor
                            )
                        }
                    }

                    // Popular Properties
                    if (popularProperties.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Most Viewed",
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
                    if (allProperties.isEmpty()) {
                        item {
                            EmptyStateSection(
                                onSurfaceVariantColor = onSurfaceVariantColor
                            )
                        }
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
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .padding(bottom = 0.dp, end = 16.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = backgroundColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
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
                tint = Color.White,
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
        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.small)
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
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Property Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                val imageUrl = property.images.values.flatten().firstOrNull()
                    ?: "https://via.placeholder.com/600x400?text=No+Image"

                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            }

            // Property Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = property.price,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = property.developer,
                    fontSize = 12.sp,
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.totalViews} views",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
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
            .padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.medium)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Find your next home",
                        style = MaterialTheme.typography.titleLarge,
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
                                MaterialTheme.colorScheme.surfaceVariant,
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary
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
    onRentClick: () -> Unit,
    onBuyClick: () -> Unit,
    onBookClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.small)
    ) {
        Text(
            text = "Browse by Category",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = GharBatoSpacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.small)
        ) {
            QuickActionCard(
                title = "Rent",
                icon = Icons.Default.Key,
                onClick = onRentClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                title = "Buy",
                icon = Icons.Default.Home,
                onClick = onBuyClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                title = "Stay",
                icon = Icons.Default.Hotel,
                onClick = onBookClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GharBatoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GharBatoDimensions.compactIcon)
            )
            Spacer(modifier = Modifier.width(GharBatoSpacing.xSmall))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            .padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(7.dp)
                )
            }

            Spacer(modifier = Modifier.width(GharBatoSpacing.small))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackgroundColor
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = onSurfaceVariantColor
                    )
                }
            }
        }

        if (showViewAll) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View All",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FeaturedPropertiesCarousel(
    properties: List<PropertyModel>,
    onPropertyClick: (PropertyModel) -> Unit,
    onFavoriteClick: (PropertyModel) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = GharBatoSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(GharBatoSpacing.medium)
    ) {
        items(properties) { property ->
            FeaturedPropertyCard(
                property = property,
                onClick = { onPropertyClick(property) },
                onFavoriteClick = { onFavoriteClick(property) }
            )
        }
    }
}

@Composable
fun FeaturedPropertyCard(
    property: PropertyModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            val imageUrl = property.images.values.flatten().firstOrNull()
                ?: "https://via.placeholder.com/600x400?text=No+Image"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(property.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Text("Featured", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (property.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (property.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Text(
                    text = property.title.ifBlank { property.developer },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Listed by ${property.ownerName.ifBlank { property.developer }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PropertyCardFacts(property = property)
            }
        }
    }
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
    onFavoriteClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GharBatoSpacing.screenHorizontal, vertical = GharBatoSpacing.small)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val imageUrl = property.images.values.flatten().firstOrNull()
                ?: "https://via.placeholder.com/600x400?text=No+Image"
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = property.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = property.price,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = when (property.marketType.lowercase()) {
                                "sell" -> "For sale"
                                "rent" -> "For rent"
                                "book" -> "Booking"
                                else -> property.marketType
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (property.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (property.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (property.isFavorite) MaterialTheme.colorScheme.primary else onSurfaceVariantColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Text(
                    text = property.title.ifBlank { property.developer },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Listed by ${property.ownerName.ifBlank { property.developer }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = onSurfaceVariantColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = property.location,
                        fontSize = 12.sp,
                        color = onSurfaceVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                PropertyCardFacts(property = property, tint = MaterialTheme.colorScheme.primary, contentColor = onSurfaceVariantColor)
            }
        }
    }
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
    onSurfaceVariantColor: Color
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
            text = "No properties available",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(GharBatoSpacing.xSmall))

        Text(
            text = "Check back later for new listings",
            fontSize = 13.sp,
            color = onSurfaceVariantColor.copy(alpha = 0.7f)
        )
    }
}
