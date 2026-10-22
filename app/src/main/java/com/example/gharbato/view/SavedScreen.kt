package com.example.gharbato.view

import com.example.gharbato.ui.theme.LocalGharBatoActionColors
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.viewmodel.SavedPropertiesViewModel
import com.example.gharbato.viewmodel.SavedPropertiesViewModelFactory

@Composable
fun SavedScreen(
    onNavigateToSearch: () -> Unit = {},
    viewModel: SavedPropertiesViewModel = viewModel(
        factory = SavedPropertiesViewModelFactory()
    )
) {
    val isDarkMode by ThemePreference.isDarkModeState.collectAsState()

    GharBatoTheme(darkTheme = isDarkMode) {
        SavedScreenContent(isDarkMode, onNavigateToSearch, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreenContent(
    isDarkMode: Boolean,
    onNavigateToSearch: () -> Unit = {},
    viewModel: SavedPropertiesViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Use the same surfaces and emphasis as discovery and property details.
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackgroundColor = MaterialTheme.colorScheme.surface
    val textColorPrimary = MaterialTheme.colorScheme.onBackground
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val soldBgColor = MaterialTheme.colorScheme.errorContainer
    val soldTextColor = MaterialTheme.colorScheme.onErrorContainer

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved homes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColorPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(backgroundColor)
        ) {
            when {
                // Loading State
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                uiState.error != null -> {
                    Column(Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(uiState.error.orEmpty(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::retry) { Text("Retry") }
                    }
                }

                // Empty State
                uiState.savedProperties.isEmpty() -> {
                    EmptySavedState(
                        onExploreClick = onNavigateToSearch,
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor,
                        errorColor = errorColor,
                        textColorPrimary = textColorPrimary,
                        textColorSecondary = textColorSecondary,
                        backgroundColor = backgroundColor,
                        surfaceVariantColor = surfaceVariantColor
                    )
                }

                // List of Saved Properties
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Count - improved from 1st and 2nd codes
                        Text(
                            text = "${uiState.savedProperties.size} saved ${if (uiState.savedProperties.size == 1) "property" else "properties"}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColorSecondary
                        )

                        // Saved Properties List
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.savedProperties,
                                key = { it.id }
                            ) { property ->
                                SavedPropertyCard(
                                    property = property,
                                    onCardClick = {
                                        val intent = Intent(context, PropertyDetailActivity::class.java)
                                        intent.putExtra("propertyId", property.id)
                                        context.startActivity(intent)
                                    },
                                    isDarkMode = isDarkMode,
                                    cardBackgroundColor = cardBackgroundColor,
                                    textColorSecondary = textColorSecondary,
                                    primaryColor = primaryColor,
                                    soldBgColor = soldBgColor,
                                    soldTextColor = soldTextColor
                                )
                            }

                            // Add "Explore More" button at the bottom of the list
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                ExploreMoreCard(
                                    onExploreClick = onNavigateToSearch,
                                    isDarkMode = isDarkMode,
                                    primaryColor = primaryColor,
                                    textColorPrimary = textColorPrimary,
                                    textColorSecondary = textColorSecondary,
                                    surfaceVariantColor = surfaceVariantColor,
                                    cardBackgroundColor = cardBackgroundColor
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // Error Message - improved theming from 1st and 2nd codes
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

}

@Composable
fun EmptySavedState(
    onExploreClick: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color,
    errorColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    backgroundColor: Color,
    surfaceVariantColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Heart Icon - with dark mode support from 1st code
        Surface(
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "No favorites",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "A place for your favorites",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = textColorPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Save homes you love and come back to them anytime.",
            fontSize = 16.sp,
            color = textColorSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExploreClick,
            colors = GharBatoButtonDefaults.primaryColors(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = LocalGharBatoActionColors.current.onContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Explore homes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LocalGharBatoActionColors.current.onContainer
            )
        }
    }
}

@Composable
fun ExploreMoreCard(
    onExploreClick: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    surfaceVariantColor: Color,
    cardBackgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExploreClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantColor
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Find your next home",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColorPrimary
                    )
                    Text(
                        text = "Explore more properties",
                        fontSize = 13.sp,
                        color = textColorSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SavedPropertyCard(
    property: PropertyModel,
    onCardClick: () -> Unit,
    isDarkMode: Boolean,
    cardBackgroundColor: Color,
    textColorSecondary: Color,
    primaryColor: Color,
    soldBgColor: Color,
    soldTextColor: Color
) {
    PropertyListingCard(
        property = property.copy(isFavorite = true),
        onClick = onCardClick,
        enabled = property.propertyStatus != "SOLD"
    )
}
