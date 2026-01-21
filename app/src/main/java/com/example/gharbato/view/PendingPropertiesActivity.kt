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

class PendingPropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PendingPropertiesScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPropertiesScreen() {
    val context = LocalContext.current
    val userRepo = remember { UserRepoImpl() }
    val currentUserId = userRepo.getCurrentUserId()

    var isLoading by remember { mutableStateOf(true) }
    var properties by remember { mutableStateOf<List<PropertyModel>>(emptyList()) }

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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pending Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${properties.size} ${if (properties.size == 1) "Property" else "Properties"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Blue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                CircularProgressIndicator(color = Blue)
            }
        } else if (properties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyPropertiesState("PENDING")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FB))
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(properties, key = { it.firebaseKey ?: it.id }) { property ->
                    PendingPropertyCard(property = property)
                }
            }
        }
    }
}

@Composable
fun PendingPropertyCard(property: PropertyModel) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                            color = Color(0xFF2C2C2C),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_location_on_24),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = property.location,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Rs ${property.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
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
                            color = Color(0xFFF0F0F0)
                        ) {
                            Text(
                                text = property.propertyType,
                                fontSize = 10.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = property.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Edit Property", color = Color(0xFF2C2C2C))
                                }
                            },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, ListingActivity::class.java).apply {
                                    putExtra("propertyId", property.firebaseKey)
                                    putExtra("isEdit", true)
                                }
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Delete Property", color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}
