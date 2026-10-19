package com.example.gharbato.view

import com.example.gharbato.ui.theme.GharBatoPalette

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
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

    // State for unsave confirmation dialog
    var showUnsaveDialog by remember { mutableStateOf(false) }
    var propertyToUnsave by remember { mutableStateOf<PropertyModel?>(null) }

    // Colors based on theme - from 1st and 2nd codes
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF8F9FA)
    val cardBackgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColorPrimary = if (isDarkMode) MaterialTheme.colorScheme.onBackground else Color.Black
    val textColorSecondary = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val primaryColor = if (isDarkMode) GharBatoPalette.brandLight else GharBatoPalette.brand
    val surfaceVariantColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else GharBatoPalette.brandContainerLight
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
    val soldBgColor = if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFD32F2F).copy(alpha = 0.1f)
    val soldTextColor = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFD32F2F)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Properties",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
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
                                    onRemoveClick = {
                                        propertyToUnsave = property
                                        showUnsaveDialog = true
                                    },
                                    isDarkMode = isDarkMode,
                                    cardBackgroundColor = cardBackgroundColor,
                                    textColorPrimary = textColorPrimary,
                                    textColorSecondary = textColorSecondary,
                                    successColor = successColor,
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
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.errorContainer else Color(0xFFD32F2F)
                ) {
                    Text(
                        text = error,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onErrorContainer else Color.White
                    )
                }
            }
        }
    }

    // Unsave Confirmation Dialog
    if (showUnsaveDialog && propertyToUnsave != null) {
        UnsavePropertyConfirmDialog(
            propertyTitle = propertyToUnsave!!.developer.ifBlank { propertyToUnsave!!.title },
            isDarkMode = isDarkMode,
            onConfirm = {
                viewModel.removeFromSaved(propertyToUnsave!!.id)
                showUnsaveDialog = false
                propertyToUnsave = null
            },
            onDismiss = {
                showUnsaveDialog = false
                propertyToUnsave = null
            }
        )
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
            modifier = Modifier.size(120.dp),
            color = if (isDarkMode) errorColor.copy(alpha = 0.1f) else Color(0xFFFFEBEE),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "No favorites",
                    modifier = Modifier.size(60.dp),
                    tint = errorColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Saved Properties",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColorPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Start exploring properties and save your favorites here for quick access later",
            fontSize = 16.sp,
            color = textColorSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExploreClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Explore Properties",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isDarkMode) CardDefaults.outlinedCardBorder() else null
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
                    color = primaryColor,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Explore More Properties",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary
                    )
                    Text(
                        text = "Find your perfect home",
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
    onRemoveClick: () -> Unit,
    isDarkMode: Boolean,
    cardBackgroundColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    successColor: Color,
    primaryColor: Color,
    soldBgColor: Color,
    soldTextColor: Color
) {
    val isSold = property.propertyStatus == "SOLD"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSold, onClick = onCardClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = if (isDarkMode) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(property.imageUrl),
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = property.price,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSold) {
                        Surface(shape = RoundedCornerShape(6.dp), color = soldBgColor) {
                            Text(
                                text = "SOLD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = soldTextColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onRemoveClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remove from favorites",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = property.title.ifBlank { property.developer },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColorPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Listed by ${property.ownerName.ifBlank { property.developer }}",
                    fontSize = 12.sp,
                    color = textColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = textColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(property.location, fontSize = 12.sp, color = textColorSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(4.dp))
                PropertyCardFacts(property = property, tint = primaryColor, contentColor = textColorSecondary)
            }
        }
    }
}

@Composable
fun PropertyStat(
    icon: ImageVector,
    text: String,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = iconColor
        )
    }
}

@Composable
fun UnsavePropertyConfirmDialog(
    propertyTitle: String,
    isDarkMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Black
    val secondaryTextColor = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFEBEE),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.HeartBroken,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Remove from Saved?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Are you sure you want to remove \"$propertyTitle\" from your saved properties?",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You can always save it again from the property details.",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remove",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Keep",
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
