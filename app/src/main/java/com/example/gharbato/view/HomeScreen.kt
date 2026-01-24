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
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.view.ui.theme.LightBlue
import com.example.gharbato.view.ui.theme.LightGreen
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
                backgroundColor = Blue
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
                        CircularProgressIndicator(color = Blue)
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

                    // Search Bar
                    item {
                        SearchBarSection(
                            onSearchClick = onNavigateToSearch,
                            onMapClick = {
                                val intent = Intent(context, FullSearchMapActivity::class.java)
                                context.startActivity(intent)
                            },
                            surfaceColor = surfaceColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
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
                                viewModel.applyFilters(PropertyFilters(marketType = "Sale"))
                                onNavigateToSearch()
                            },
                            onBookClick = {
                                viewModel.applyFilters(PropertyFilters(marketType = "Book"))
                                onNavigateToSearch()
                            }
                        )
                    }

                    // Statistics Section
                    item {
                        PropertyStatsSection(
                            totalProperties = allProperties.size,
                            rentProperties = allProperties.count { it.marketType.equals("Rent", ignoreCase = true) },
                            saleProperties = allProperties.count { it.marketType.equals("Sale", ignoreCase = true) },
                            bookProperties = allProperties.count { it.marketType.equals("Book", ignoreCase = true) },
                            surfaceColor = surfaceColor,
                            onBackgroundColor = onBackgroundColor,
                            onSurfaceVariantColor = onSurfaceVariantColor
                        )
                    }

                    // Featured Properties (Algorithm-based)
                    if (featuredProperties.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Featured Properties",
                                subtitle = "Top rated properties based on engagement",
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
                                subtitle = "Recently added properties",
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
                                subtitle = "Properties everyone's checking out",
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
fun PropertyStatsSection(
    totalProperties: Int,
    rentProperties: Int,
    saleProperties: Int,
    bookProperties: Int,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Properties Available",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = onSurfaceVariantColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total", totalProperties, Color(0xFF2196F3), onBackgroundColor)
                StatItem("Rent", rentProperties, Color(0xFF4CAF50), onBackgroundColor)
                StatItem("Sale", saleProperties, Color(0xFFFF9800), onBackgroundColor)
                StatItem("Book", bookProperties, Color(0xFF9C27B0), onBackgroundColor)
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, color: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.7f)
        )
    }
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    .height(120.dp)
            ) {
                val imageUrl = property.images.values.flatten().firstOrNull()
                    ?: "https://via.placeholder.com/600x400?text=No+Image"

                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Views Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = property.totalViews.toString(),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Property Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = property.price,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
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
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Blue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Blue.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    }
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello 👋",
                        fontSize = 16.sp,
                        color = onBackgroundColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Find Your Dream Home",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor
                    )
                }

                // Notification Bell
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadCount > 0) Blue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = Color.Red
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarSection(
    onSearchClick: () -> Unit,
    onMapClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clickable(onClick = onSearchClick),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = onSurfaceVariantColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search location, property...",
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor
                )
            }
        }

        // Map Button
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onMapClick),
            shape = RoundedCornerShape(16.dp),
            color = Blue,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map View",
                    tint = Color.White
                )
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
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Browse by Category",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "For Rent",
                subtitle = "Monthly rentals",
                icon = Icons.Default.Key,
                color = Color(0xFF4CAF50),
                onClick = onRentClick,
                modifier = Modifier.weight(1f),
                surfaceVariantColor = surfaceVariantColor
            )

            QuickActionCard(
                title = "For Sale",
                subtitle = "Buy property",
                icon = Icons.Default.Home,
                color = Color(0xFF2196F3),
                onClick = onBuyClick,
                modifier = Modifier.weight(1f),
                surfaceVariantColor = surfaceVariantColor
            )

            QuickActionCard(
                title = "For Book",
                subtitle = "Short stays",
                icon = Icons.Default.Hotel,
                color = Color(0xFFFF9800),
                onClick = onBookClick,
                modifier = Modifier.weight(1f),
                surfaceVariantColor = surfaceVariantColor
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceVariantColor: Color
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background == Color(0xFF121212)) {
                color.copy(alpha = 0.2f)
            } else {
                color.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, surfaceVariantColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Blue.copy(alpha = if (MaterialTheme.colorScheme.background == Color(0xFF121212)) 0.2f else 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = onSurfaceVariantColor
                )
            }
        }

        if (showViewAll) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    color = Blue
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View All",
                    tint = Blue,
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            .width(300.dp)
            .height(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Property Image
            val imageUrl = property.images.values.flatten().firstOrNull()
                ?: "https://via.placeholder.com/600x400?text=No+Image"

            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = property.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Featured Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Featured",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Featured",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    imageVector = if (property.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (property.isFavorite) Color.Red else Color.Gray
                )
            }

            // Property Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = property.price,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = property.developer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PropertyFeature(Icons.Default.Bed, "${property.bedrooms}")
                    PropertyFeature(Icons.Default.Bathroom, "${property.bathrooms}")
                    PropertyFeature(Icons.Default.SquareFoot, property.sqft)
                }
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Property Image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val imageUrl = property.images.values.flatten().firstOrNull()
                    ?: "https://via.placeholder.com/600x400?text=No+Image"

                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Market Type Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    color = when (property.marketType.lowercase()) {
                        "rent" -> Color(0xFF4CAF50)
                        "sale" -> Color(0xFF2196F3)
                        "book" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = property.marketType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // NEW Badge
                val isNew = (System.currentTimeMillis() - property.updatedAt) < (3 * 24 * 60 * 60 * 1000)
                if (isNew) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "NEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Property Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = property.price,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = property.developer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = property.location,
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(Icons.Default.Bed, "${property.bedrooms}", onSurfaceVariantColor)
                    InfoChip(Icons.Default.Bathroom, "${property.bathrooms}", onSurfaceVariantColor)
                    InfoChip(Icons.Default.SquareFoot, property.sqft, onSurfaceVariantColor)
                }
            }

            // Favorite Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (property.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (property.isFavorite) Color.Red else onSurfaceVariantColor
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String, chipColor: Color) {
    Surface(
        color = if (MaterialTheme.colorScheme.background == Color(0xFF121212)) {
            chipColor.copy(alpha = 0.2f)
        } else {
            chipColor.copy(alpha = 0.1f)
        },
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = chipColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = onSurfaceVariantColor.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No properties available",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check back later for new listings",
            fontSize = 14.sp,
            color = onSurfaceVariantColor.copy(alpha = 0.7f)
        )
    }
}