package com.example.gharbato.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.SortOption

/**
 * Bottom sheet for selecting sort options
 * This is the VIEW in MVVM architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort By",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Divider(color = Color.Gray.copy(0.3f))

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                // Popularity
                SortOptionItem(
                    icon = Icons.Default.Star,
                    sortOption = SortOption.POPULARITY,
                    isSelected = selectedSort == SortOption.POPULARITY,
                    onClick = {
                        selectedSort = SortOption.POPULARITY
                        onSortSelected(SortOption.POPULARITY)
                        onDismiss()
                    }
                )

                // Price - Low to High
                SortOptionItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    sortOption = SortOption.PRICE_LOW_TO_HIGH,
                    isSelected = selectedSort == SortOption.PRICE_LOW_TO_HIGH,
                    onClick = {
                        selectedSort = SortOption.PRICE_LOW_TO_HIGH
                        onSortSelected(SortOption.PRICE_LOW_TO_HIGH)
                        onDismiss()
                    }
                )

                // Price - High to Low
                SortOptionItem(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    sortOption = SortOption.PRICE_HIGH_TO_LOW,
                    isSelected = selectedSort == SortOption.PRICE_HIGH_TO_LOW,
                    onClick = {
                        selectedSort = SortOption.PRICE_HIGH_TO_LOW
                        onSortSelected(SortOption.PRICE_HIGH_TO_LOW)
                        onDismiss()
                    }
                )

                // Area - Small to Large
                SortOptionItem(
                    icon = Icons.Default.ZoomOut,
                    sortOption = SortOption.AREA_SMALL_TO_LARGE,
                    isSelected = selectedSort == SortOption.AREA_SMALL_TO_LARGE,
                    onClick = {
                        selectedSort = SortOption.AREA_SMALL_TO_LARGE
                        onSortSelected(SortOption.AREA_SMALL_TO_LARGE)
                        onDismiss()
                    }
                )

                // Area - Large to Small
                SortOptionItem(
                    icon = Icons.Default.ZoomIn,
                    sortOption = SortOption.AREA_LARGE_TO_SMALL,
                    isSelected = selectedSort == SortOption.AREA_LARGE_TO_SMALL,
                    onClick = {
                        selectedSort = SortOption.AREA_LARGE_TO_SMALL
                        onSortSelected(SortOption.AREA_LARGE_TO_SMALL)
                        onDismiss()
                    }
                )

                // Date - Newest
                SortOptionItem(
                    icon = Icons.Default.NewReleases,
                    sortOption = SortOption.DATE_NEWEST,
                    isSelected = selectedSort == SortOption.DATE_NEWEST,
                    onClick = {
                        selectedSort = SortOption.DATE_NEWEST
                        onSortSelected(SortOption.DATE_NEWEST)
                        onDismiss()
                    }
                )

                // Date - Oldest
                SortOptionItem(
                    icon = Icons.Default.History,
                    sortOption = SortOption.DATE_OLDEST,
                    isSelected = selectedSort == SortOption.DATE_OLDEST,
                    onClick = {
                        selectedSort = SortOption.DATE_OLDEST
                        onSortSelected(SortOption.DATE_OLDEST)
                        onDismiss()
                    }
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Individual sort option item
 */
@Composable
fun SortOptionItem(
    icon: ImageVector,
    sortOption: SortOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF2196F3) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = sortOption.displayName,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF2196F3) else Color.Black
                    )

                    Text(
                        text = sortOption.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}