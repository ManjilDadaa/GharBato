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

class AllPropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AllPropertiesScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPropertiesScreen() {
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All My Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                EmptyPropertiesState("ALL")
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
                items(properties) { property ->
                    AllPropertyCard(property = property)
                }
            }
        }
    }
}

@Composable
fun AllPropertyCard(property: PropertyModel) {
    val context = LocalContext.current

    val statusColor = when (property.status) {
        PropertyStatus.APPROVED -> Color(0xFF4CAF50)
        PropertyStatus.PENDING -> Color(0xFFFF9800)
        PropertyStatus.REJECTED -> Color(0xFFD32F2F)
        else -> Blue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, PropertyDetailsActivity::class.java).apply {
                    putExtra("propertyId", property.id)
                }
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
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
                        color = statusColor.copy(alpha = 0.1f)
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
