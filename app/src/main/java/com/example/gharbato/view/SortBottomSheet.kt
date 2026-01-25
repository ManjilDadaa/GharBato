package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.SortOption
import com.example.gharbato.ui.theme.Blue

/**
 * Enhanced bottom sheet for selecting sort options with comprehensive dark theme support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean = false
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    // Enhanced theme colors with better contrast ratios
    val themeColors = remember(isDarkMode) {
        if (isDarkMode) {
            DarkThemeColors(
                backgroundColor = Color(0xFF121212),
                surfaceColor = Color(0xFF1E1E1E),
                textColor = Color(0xFFE0E0E0),
                secondaryTextColor = Color(0xFFA0A0A0),
                selectedBackgroundColor = Color(0xFF1E3A5F).copy(alpha = 0.7f),
                selectedTextColor = Color(0xFF64B5F6),
                dividerColor = Color(0xFF424242),
                iconColor = Color(0xFFB0B0B0),
                primaryColor = Color(0xFF64B5F6),
                closeIconColor = Color(0xFFB0B0B0),
                checkIconColor = Color(0xFF4FC3F7),
                dragHandleColor = Color(0xFF616161),
                rippleColor = Color(0x1AFFFFFF)
            )
        } else {
            LightThemeColors(
                backgroundColor = Color.White,
                surfaceColor = Color(0xFFF5F5F5),
                textColor = Color(0xFF212121),
                secondaryTextColor = Color(0xFF757575),
                selectedBackgroundColor = Color(0xFFE3F2FD),
                selectedTextColor = Color(0xFF1976D2),
                dividerColor = Color(0xFFE0E0E0),
                iconColor = Color(0xFF757575),
                primaryColor = Blue,
                closeIconColor = Color(0xFF616161),
                checkIconColor = Color(0xFF2196F3),
                dragHandleColor = Color(0xFFBDBDBD),
                rippleColor = Color(0x1A000000)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = themeColors.backgroundColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Divider(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    color = themeColors.dragHandleColor,
                    thickness = 4.dp
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Header with elevation
            Surface(
                color = themeColors.surfaceColor,
                tonalElevation = 2.dp
            ) {
                Column {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort Properties",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = themeColors.textColor
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = themeColors.closeIconColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Divider(
                        color = themeColors.dividerColor,
                        thickness = 0.8.dp
                    )
                }
            }

            // Scrollable content with proper padding
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false)
                    .padding(vertical = 8.dp)
            ) {
                // Sort options list
                val sortOptions = listOf(
                    SortOption.POPULARITY to Icons.Default.Star,
                    SortOption.PRICE_LOW_TO_HIGH to Icons.AutoMirrored.Filled.TrendingUp,
                    SortOption.PRICE_HIGH_TO_LOW to Icons.AutoMirrored.Filled.TrendingDown,
                    SortOption.AREA_SMALL_TO_LARGE to Icons.Default.Expand,
                    SortOption.AREA_LARGE_TO_SMALL to Icons.Default.Compress,
                    SortOption.DATE_NEWEST to Icons.Default.NewReleases,
                    SortOption.DATE_OLDEST to Icons.Default.History
                )

                sortOptions.forEach { (option, icon) ->
                    SortOptionItem(
                        icon = icon,
                        sortOption = option,
                        isSelected = selectedSort == option,
                        onClick = {
                            selectedSort = option
                            onSortSelected(option)
                            onDismiss()
                        },
                        themeColors = themeColors
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Bottom spacing for navigation bar
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Enhanced sort option item with better visual feedback
 */
@Composable
fun SortOptionItem(
    icon: ImageVector,
    sortOption: SortOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    themeColors: ThemeColors
) {
    // Enhanced interaction handling
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColors.selectedBackgroundColor
            else Color.Transparent,
            contentColor = if (isSelected) themeColors.selectedTextColor
            else themeColors.textColor
        ),
        elevation = if (isSelected) CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        ) else CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isSelected) themeColors.selectedTextColor.copy(alpha = 0.1f)
                            else themeColors.iconColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) themeColors.selectedTextColor
                        else themeColors.iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text content
                Column {
                    Text(
                        text = sortOption.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold
                            else FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = if (isSelected) themeColors.selectedTextColor
                        else themeColors.textColor
                    )

                    Text(
                        text = sortOption.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) themeColors.selectedTextColor.copy(alpha = 0.8f)
                        else themeColors.secondaryTextColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = themeColors.checkIconColor,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Theme color data classes for better organization
 */
sealed class ThemeColors {
    abstract val backgroundColor: Color
    abstract val surfaceColor: Color
    abstract val textColor: Color
    abstract val secondaryTextColor: Color
    abstract val selectedBackgroundColor: Color
    abstract val selectedTextColor: Color
    abstract val dividerColor: Color
    abstract val iconColor: Color
    abstract val primaryColor: Color
    abstract val closeIconColor: Color
    abstract val checkIconColor: Color
    abstract val dragHandleColor: Color
    abstract val rippleColor: Color
}

data class DarkThemeColors(
    override val backgroundColor: Color = Color(0xFF121212),
    override val surfaceColor: Color = Color(0xFF1E1E1E),
    override val textColor: Color = Color(0xFFE0E0E0),
    override val secondaryTextColor: Color = Color(0xFFA0A0A0),
    override val selectedBackgroundColor: Color = Color(0xFF1E3A5F).copy(alpha = 0.7f),
    override val selectedTextColor: Color = Color(0xFF64B5F6),
    override val dividerColor: Color = Color(0xFF424242),
    override val iconColor: Color = Color(0xFFB0B0B0),
    override val primaryColor: Color = Color(0xFF64B5F6),
    override val closeIconColor: Color = Color(0xFFB0B0B0),
    override val checkIconColor: Color = Color(0xFF4FC3F7),
    override val dragHandleColor: Color = Color(0xFF616161),
    override val rippleColor: Color = Color(0x1AFFFFFF)
) : ThemeColors()

data class LightThemeColors(
    override val backgroundColor: Color = Color.White,
    override val surfaceColor: Color = Color(0xFFF5F5F5),
    override val textColor: Color = Color(0xFF212121),
    override val secondaryTextColor: Color = Color(0xFF757575),
    override val selectedBackgroundColor: Color = Color(0xFFE3F2FD),
    override val selectedTextColor: Color = Color(0xFF1976D2),
    override val dividerColor: Color = Color(0xFFE0E0E0),
    override val iconColor: Color = Color(0xFF757575),
    override val primaryColor: Color = Blue,
    override val closeIconColor: Color = Color(0xFF616161),
    override val checkIconColor: Color = Color(0xFF2196F3),
    override val dragHandleColor: Color = Color(0xFFBDBDBD),
    override val rippleColor: Color = Color(0x1A000000)
) : ThemeColors()


val SortOption.displayName: String
    get() = when (this) {
        SortOption.POPULARITY -> "Most Popular"
        SortOption.PRICE_LOW_TO_HIGH -> "Price: Low to High"
        SortOption.PRICE_HIGH_TO_LOW -> "Price: High to Low"
        SortOption.AREA_SMALL_TO_LARGE -> "Area: Small to Large"
        SortOption.AREA_LARGE_TO_SMALL -> "Area: Large to Small"
        SortOption.DATE_NEWEST -> "Newest First"
        SortOption.DATE_OLDEST -> "Oldest First"
    }

val SortOption.description: String
    get() = when (this) {
        SortOption.POPULARITY -> "Sort by user ratings and reviews"
        SortOption.PRICE_LOW_TO_HIGH -> "Affordable properties first"
        SortOption.PRICE_HIGH_TO_LOW -> "Premium properties first"
        SortOption.AREA_SMALL_TO_LARGE -> "Compact spaces first"
        SortOption.AREA_LARGE_TO_SMALL -> "Spacious properties first"
        SortOption.DATE_NEWEST -> "Recently added properties"
        SortOption.DATE_OLDEST -> "Older properties first"
    }