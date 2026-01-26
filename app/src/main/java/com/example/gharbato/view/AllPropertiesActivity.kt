package com.example.gharbato.view

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Info
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

class AllPropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            AllPropertiesScreen(isDarkMode = isDarkMode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPropertiesScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    val userRepo = remember { UserRepoImpl() }
    val currentUserId = userRepo.getCurrentUserId()

    var isLoading by remember { mutableStateOf(true) }
    var properties by remember { mutableStateOf<List<PropertyModel>>(emptyList()) }

    // Statistics
    val approvedCount = remember(properties) {
        properties.count { it.status == PropertyStatus.APPROVED }
    }
    val pendingCount = remember(properties) {
        properties.count { it.status == PropertyStatus.PENDING }
    }
    val rejectedCount = remember(properties) {
        properties.count { it.status == PropertyStatus.REJECTED }
    }

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
                            Log.e("AllProperties", "Error: ${e.message}")
                            null
                        }
                    }

                    properties = allProperties.filter { property ->
                        property.ownerId == currentUserId
                    }.sortedByDescending { it.createdAt }

                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AllProperties", "Error: ${error.message}")
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
                            "All My Properties",
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
                ),
                actions = {
                    IconButton(onClick = {
                        // Show stats or filter options
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Statistics",
                            tint = primaryColor
                        )
                    }
                }
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
                        text = "No Properties Found",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You don't have any properties yet.",
                        fontSize = 14.sp,
                        color = secondaryTextColor
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding)
            ) {
                // Statistics Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fixed: Use Modifier.weight(1f) as property, not function
                    StatChip(
                        count = approvedCount,
                        label = "Approved",
                        color = successColor,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        count = pendingCount,
                        label = "Pending",
                        color = warningColor,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        count = rejectedCount,
                        label = "Rejected",
                        color = errorColor,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(properties, key = { it.firebaseKey ?: it.id }) { property ->
                        AllPropertyCard(
                            property = property,
                            isDarkMode = isDarkMode,
                            cardBackgroundColor = cardBackgroundColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            primaryColor = primaryColor,
                            successColor = successColor,
                            warningColor = warningColor,
                            errorColor = errorColor,
                            chipBackgroundColor = chipBackgroundColor,
                            chipTextColor = chipTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    count: Int,
    label: String,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (isDarkMode) 0.15f else 0.1f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
            )
        }
    }
}

@Composable
fun AllPropertyCard(
    property: PropertyModel,
    isDarkMode: Boolean,
    cardBackgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    primaryColor: Color,
    successColor: Color,
    warningColor: Color,
    errorColor: Color,
    chipBackgroundColor: Color,
    chipTextColor: Color
) {
    val context = LocalContext.current

    val statusColor = when (property.status) {
        PropertyStatus.APPROVED -> successColor
        PropertyStatus.PENDING -> warningColor
        PropertyStatus.REJECTED -> errorColor
        else -> primaryColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 2.dp else 2.dp)
    ) {
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

            // Fixed: Use Modifier.weight(1f) as property, not function
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
                        color = statusColor.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
                    ) {
                        Text(
                            text = property.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}