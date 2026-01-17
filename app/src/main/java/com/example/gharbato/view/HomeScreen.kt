package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.R
import com.example.gharbato.model.SortOption
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Purple
import com.example.gharbato.viewmodel.PropertyViewModel
import com.example.gharbato.viewmodel.PropertyViewModelFactory
import com.example.gharbato.viewmodel.UserViewModelProvider

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

    // Active quick filter
    var activeQuickFilter by remember { mutableStateOf<QuickFilter?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Start observing notifications
    LaunchedEffect(Unit) {
        userViewModel.startObservingNotifications()
    }

    // Keep observers running when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            // Don't stop observers - keep them running for real-time updates
        }
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, ListingActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = "Add Property"
                        )
                    }
                },
                actions = {
                    // Notification icon with badge
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = {
                                val intent = Intent(context, NotificationActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_notifications_24),
                                contentDescription = "Notifications",
                                tint = Blue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Badge - only show if count > 0
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 6.dp)
                                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .border(2.dp, Color.White, CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
    ) { padding ->
        // Show loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FB))
                    .padding(top = padding.calculateTopPadding()),
            ) {
                // Search Button Card (navigates to SearchScreen)
                item {
                    SearchButtonCard(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Quick Filter Chips
                item {
                    QuickFiltersRow(
                        activeFilter = activeQuickFilter,
                        onFilterClick = { filter ->
                            if (activeQuickFilter == filter) {
                                // Deselect and show all
                                activeQuickFilter = null
                                viewModel.updateSort(SortOption.DATE_NEWEST)
                            } else {
                                // Apply quick filter
                                activeQuickFilter = filter
                                when (filter) {
                                    QuickFilter.TRENDING -> viewModel.updateSort(SortOption.POPULARITY)
                                    QuickFilter.NEWEST -> viewModel.updateSort(SortOption.DATE_NEWEST)
                                    QuickFilter.NEARBY -> {
                                        // TODO: Implement nearby sorting based on user location
                                        // For now, just sort by newest
                                        viewModel.updateSort(SortOption.DATE_NEWEST)
                                    }
                                    QuickFilter.PRICE_RANGE -> {
                                        // Navigate to SearchScreen with filters open
                                        onNavigateToSearch()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Properties count and current filter indicator
                if (uiState.properties.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.properties.size} Properties",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            if (activeQuickFilter != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Purple.copy(0.1f)
                                ) {
                                    Text(
                                        text = when (activeQuickFilter) {
                                            QuickFilter.TRENDING -> "Trending"
                                            QuickFilter.NEWEST -> "Newest"
                                            QuickFilter.NEARBY -> "Nearby"
                                            QuickFilter.PRICE_RANGE -> "Price Range"
                                            null -> ""
                                        },
                                        fontSize = 12.sp,
                                        color = Purple,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Property Cards
                items(uiState.properties) { property ->
                    PropertyCard(
                        property = property,
                        onClick = {
                            val intent = Intent(context, PropertyDetailActivity::class.java)
                            intent.putExtra("propertyId", property.id)
                            context.startActivity(intent)
                        },
                        onFavoriteClick = { prop ->
                            viewModel.toggleFavorite(prop)
                        }
                    )
                }

                // Empty state
                if (uiState.properties.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchButtonCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        shadowElevation = 0.dp
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
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Search for properties...",
                fontSize = 15.sp,
                color = Color.Gray
            )
        }
    }
}

enum class QuickFilter {
    TRENDING, NEWEST, NEARBY, PRICE_RANGE
}

@Composable
fun QuickFiltersRow(
    activeFilter: QuickFilter?,
    onFilterClick: (QuickFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        QuickFilter.TRENDING to R.drawable.baseline_trending_up_24,
        QuickFilter.NEWEST to R.drawable.baseline_star_border_purple500_24,
        QuickFilter.NEARBY to R.drawable.outline_location_on_24,
        QuickFilter.PRICE_RANGE to R.drawable.baseline_currency_rupee_24
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        filters.forEach { (filter, icon) ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterClick(filter) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (filter) {
                                QuickFilter.TRENDING -> "Trending"
                                QuickFilter.NEWEST -> "New"
                                QuickFilter.NEARBY -> "Nearby"
                                QuickFilter.PRICE_RANGE -> "Price"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Blue,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_home_24),
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No properties available",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check back later for new listings",
            fontSize = 14.sp,
            color = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

@Composable
@Preview
fun HomeScreenPreview() {
    HomeScreen()
}