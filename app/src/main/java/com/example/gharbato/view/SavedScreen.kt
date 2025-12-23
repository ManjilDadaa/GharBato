package com.example.gharbato.view

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl
import com.example.gharbato.ui.view.PropertyDetailActivity
import com.example.gharbato.viewmodel.SavedPropertiesViewModel
import com.example.gharbato.viewmodel.SavedPropertiesViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedPropertiesViewModel = viewModel(
        factory = SavedPropertiesViewModelFactory(SavedPropertiesRepositoryImpl()) // ✅ Add factory
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Properties",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(Color(0xFFF8F9FA))
        ) {
            when {
                // Loading State
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Empty State
                uiState.savedProperties.isEmpty() -> {
                    EmptySavedState()
                }

                // List of Saved Properties
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Count
                        Text(
                            text = "${uiState.savedProperties.size} saved ${if (uiState.savedProperties.size == 1) "property" else "properties"}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Gray
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
                                        viewModel.removeFromSaved(property.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = error)
                }
            }
        }
    }
}

@Composable
fun EmptySavedState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Heart Icon
        Surface(
            modifier = Modifier.size(120.dp),
            color = Color(0xFFFFEBEE),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "No favorites",
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFFE91E63)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Saved Properties",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Start exploring properties and save your favorites here for quick access later",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Navigate to search */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Explore Properties",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SavedPropertyCard(
    property: PropertyModel,
    onCardClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Property Image
            Image(
                painter = rememberAsyncImagePainter(property.imageUrl),
                contentDescription = property.title,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Property Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = property.developer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = property.price,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = property.location,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                // Property Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PropertyStat(Icons.Default.Home, property.sqft)
                    PropertyStat(Icons.Default.Info, "${property.bedrooms} BD")
                    PropertyStat(Icons.Default.Star, "${property.bathrooms} BA")
                }
            }

            // Remove Button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PropertyStat(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}