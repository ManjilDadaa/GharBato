package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gharbato.R
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import com.example.gharbato.ui.theme.Blue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.gharbato.repository.UserRepoImpl
import android.util.Log
import com.example.gharbato.utils.SystemBarUtils

class PendingPropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            PendingPropertiesScreen(isDarkMode = isDarkMode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPropertiesScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    val userRepo = remember { UserRepoImpl() }
    val currentUserId = userRepo.getCurrentUserId()

    var isLoading by remember { mutableStateOf(true) }
    var properties by remember { mutableStateOf<List<PropertyModel>>(emptyList()) }

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FB)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF2C2C2C)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val chipBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    val chipTextColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF666666)
    val primaryColor = if (isDarkMode) Color(0xFF82B1FF) else Blue
    val successColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
    val warningColor = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFFF9800)
    val errorColor = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val infoColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)

    // Dropdown menu specific colors
    val dropdownContainerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    val dropdownTextColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF333333)
    val dropdownDividerColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val database = FirebaseDatabase.getInstance()
            val propertiesRef = database.getReference("Property")

            propertiesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allProperties = snapshot.children.mapNotNull { child ->
                        try {
                            child.getValue(PropertyModel::class.java)?.copy(firebaseKey = child.key)
                        } catch (e: Exception) {
                            Log.e("PendingProperties", "Error: ${e.message}")
                            null
                        }
                    }

                    properties = allProperties.filter { property ->
                        property.ownerId == currentUserId && property.status == PropertyStatus.PENDING
                    }.sortedByDescending { it.createdAt }

                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PendingProperties", "Error: ${error.message}")
                    isLoading = false
                }
            })
        } else {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Pending Properties",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                        Text(
                            "${properties.size} ${if (properties.size == 1) "Property" else "Properties"}",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (properties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_home_24),
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pending Properties",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You don't have any properties pending review.",
                        fontSize = 14.sp,
                        color = secondaryTextColor
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(properties, key = { it.firebaseKey ?: it.id }) { property ->
                    PendingPropertyCard(
                        property = property,
                        isDarkMode = isDarkMode,
                        cardBackgroundColor = cardBackgroundColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        primaryColor = primaryColor,
                        warningColor = warningColor,
                        errorColor = errorColor,
                        chipBackgroundColor = chipBackgroundColor,
                        chipTextColor = chipTextColor,
                        dropdownContainerColor = dropdownContainerColor,
                        dropdownTextColor = dropdownTextColor,
                        dropdownDividerColor = dropdownDividerColor
                    )
                }
            }
        }
    }
}

@Composable
fun PendingPropertyCard(
    property: PropertyModel,
    isDarkMode: Boolean,
    cardBackgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    primaryColor: Color,
    warningColor: Color,
    errorColor: Color,
    chipBackgroundColor: Color,
    chipTextColor: Color,
    dropdownContainerColor: Color,
    dropdownTextColor: Color,
    dropdownDividerColor: Color
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 2.dp else 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(context, PropertyDetailsActivity::class.java).apply {
                            putExtra("propertyId", property.id)
                        }
                        context.startActivity(intent)
                    }
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = property.imageUrl,
                    contentDescription = property.title,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.baseline_home_24)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = property.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_location_on_24),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = secondaryTextColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = property.location,
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Rs ${property.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipBackgroundColor
                        ) {
                            Text(
                                text = property.propertyType,
                                fontSize = 10.sp,
                                color = chipTextColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = warningColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
                        ) {
                            Text(
                                text = property.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = warningColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = secondaryTextColor
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(dropdownContainerColor)
                    ) {
                        // Edit option
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Edit Property",
                                        color = dropdownTextColor
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, ListingActivity::class.java).apply {
                                    putExtra("propertyId", property.firebaseKey)
                                    putExtra("isEdit", true)
                                }
                                context.startActivity(intent)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = dropdownTextColor,
                                leadingIconColor = primaryColor
                            )
                        )

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = dropdownDividerColor,
                            thickness = 0.5.dp
                        )

                        // Delete option
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = errorColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Delete Property",
                                        color = errorColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = errorColor,
                                leadingIconColor = errorColor
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Property?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${property.title}\"? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        property.firebaseKey?.let { key ->
                            FirebaseDatabase.getInstance()
                                .getReference("Property")
                                .child(key)
                                .removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Property deleted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Failed to delete property",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = secondaryTextColor
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            textContentColor = textColor
        )
    }
}