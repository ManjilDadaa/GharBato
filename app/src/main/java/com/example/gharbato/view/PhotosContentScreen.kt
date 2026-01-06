package com.example.gharbato.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.model.ImageCategory
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@Composable
fun PhotosContentScreen(
    imageCategories: List<ImageCategory>,
    onCategoriesChange: (List<ImageCategory>) -> Unit
) {
    val totalImages = imageCategories.sumOf { it.images.size }
    val requiredCategoriesFilled = imageCategories
        .filter { it.isRequired }
        .all { it.images.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Property Photos",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add photos to attract more buyers",
                    fontSize = 14.sp,
                    color = Gray
                )
            }

            // Photo counter badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Blue.copy(alpha = 0.1f)
            ) {
                Text(
                    "$totalImages Photos",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Blue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Requirements Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (requiredCategoriesFilled)
                    Color(0xFFE8F5E9)
                else
                    Color(0xFFFFF3E0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (requiredCategoriesFilled)
                            R.drawable.baseline_check_24
                        else
                            R.drawable.baseline_info_24
                    ),
                    contentDescription = null,
                    tint = if (requiredCategoriesFilled)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (requiredCategoriesFilled)
                        "Required photos added ✓"
                    else
                        "Cover Photo and Bedroom photos are required",
                    fontSize = 13.sp,
                    color = if (requiredCategoriesFilled)
                        Color(0xFF2E7D32)
                    else
                        Color(0xFFE65100)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Categories
        imageCategories.forEachIndexed { index, category ->
            ImageCategorySection(
                category = category,
                onImagesSelected = { uris ->
                    val updatedCategories = imageCategories.toMutableList()
                    val currentImages = category.images.toMutableList()

                    //  Add only images that aren't already in the list
                    uris.forEach { uri ->
                        val uriString = uri.toString()
                        if (!currentImages.contains(uriString) && currentImages.size < category.maxImages) {
                            currentImages.add(uriString)
                        }
                    }

                    updatedCategories[index] = category.copy(images = currentImages)
                    onCategoriesChange(updatedCategories)
                },
                onImageRemoved = { uriString ->
                    val updatedCategories = imageCategories.toMutableList()
                    val currentImages = category.images.toMutableList()
                    currentImages.remove(uriString)
                    updatedCategories[index] = category.copy(images = currentImages)
                    onCategoriesChange(updatedCategories)
                }
            )

            if (index < imageCategories.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_lightbulb_24),
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Photo Tips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "Use good lighting - natural light works best",
                    "Take photos from multiple angles",
                    "Clean and declutter spaces before shooting",
                    "Show unique features and recent renovations",
                    "Horizontal orientation works best"
                ).forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "•",
                            fontSize = 16.sp,
                            color = Blue,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            tip,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ImageCategorySection(
    category: ImageCategory,
    onImagesSelected: (List<Uri>) -> Unit,
    onImageRemoved: (String) -> Unit
) {
    //  For cover photo, only allow single selection
    val isCoverPhoto = category.id == "cover"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = if (isCoverPhoto) {
            ActivityResultContracts.GetContent() //  Single selection for cover
        } else {
            ActivityResultContracts.GetMultipleContents() // Multiple for others
        }
    ) { result ->
        when (result) {
            is Uri -> {
                // Single URI (cover photo)
                if (!category.images.contains(result.toString())) {
                    onImagesSelected(listOf(result))
                }
            }
            is List<*> -> {
                // Multiple URIs (other categories)
                val uris = result.filterIsInstance<Uri>()
                if (uris.isNotEmpty()) {
                    //  Filter out already selected images
                    val newUris = uris.filter { uri ->
                        !category.images.contains(uri.toString())
                    }
                    if (newUris.isNotEmpty()) {
                        onImagesSelected(newUris)
                    }
                }
            }
        }
    }

    val canAddMore = category.images.size < category.maxImages

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (category.isRequired && category.images.isEmpty())
                Color(0xFFFF9800)
            else
                Color(0xFFE0E0E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Blue.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(category.icon),
                                contentDescription = null,
                                tint = Blue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                category.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (category.isRequired) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFF5252).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "Required",
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        ),
                                        fontSize = 10.sp,
                                        color = Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            category.description,
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }
                }

                // Image count
                Text(
                    "${category.images.size}/${category.maxImages}",
                    fontSize = 13.sp,
                    color = if (category.images.size == category.maxImages)
                        Blue
                    else
                        Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Images Display
            if (category.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = category.images,
                        key = { it }
                    ) { uriString ->
                        ImageThumbnail(
                            uriString = uriString,
                            onRemove = { onImageRemoved(uriString) }
                        )
                    }

                    // Add more button (only if not cover photo and can add more)
                    if (canAddMore && !isCoverPhoto) {
                        item {
                            AddImageButton(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            )
                        }
                    }
                }
            } else {
                // Empty state - Add first image
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFAFAFA)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = Blue.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add ${category.title}",
                            color = Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageThumbnail(
    uriString: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = uriString,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_close_24),
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .border(
                width = 1.5.dp,
                color = Blue.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_add_24),
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(32.dp)
            )
            Text(
                "Add More",
                fontSize = 11.sp,
                color = Blue,
                textAlign = TextAlign.Center
            )
        }
    }
}