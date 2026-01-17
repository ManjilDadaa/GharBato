package com.example.gharbato.view

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.model.SearchHistory
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray



@Composable
fun SearchHistorySection(
    searchHistory: List<SearchHistory>,
    isLoading: Boolean,
    onHistoryItemClick: (SearchHistory) -> Unit,
    onHistoryItemDelete: (SearchHistory) -> Unit,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Search History",
                    tint = Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Recent Searches",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            if (searchHistory.isNotEmpty()) {
                TextButton(onClick = onClearAllClick) {
                    Text(
                        text = "Clear All",
                        fontSize = 14.sp,
                        color = Blue
                    )
                }
            }
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Blue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            searchHistory.isEmpty() -> {
                EmptySearchHistoryView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = searchHistory,
                        key = { it.id }
                    ) { history ->
                        SearchHistoryItem(
                            searchHistory = history,
                            onClick = { onHistoryItemClick(history) },
                            onDelete = { onHistoryItemDelete(history) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    searchHistory: SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = if (searchHistory.isLocationSearch()) {
                    Icons.Default.LocationOn
                } else {
                    Icons.Default.Search
                },
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = searchHistory.getDisplayText(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val description = searchHistory.getDescription()
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time ago
            Text(
                text = searchHistory.getTimeAgo(),
                fontSize = 11.sp,
                color = Gray.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySearchHistoryView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No search history",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your recent searches will appear here",
            fontSize = 13.sp,
            color = Gray.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFFEF5350)
            )
        },
        title = {
            Text(
                text = "Clear Search History?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will delete all your recent searches. This action cannot be undone.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF5350)
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}