package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.LightBlue

class ReportedPropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportedPropertiesScreen()
        }
    }
}

data class ReportedProperty(
    val propertyId: String,
    val propertyTitle: String,
    val propertyImage: String,
    val ownerName: String,
    val reportCount: Int,
    val reportReason: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedPropertiesScreen() {
    val properties = remember {
        listOf(
            ReportedProperty(
                "p1",
                "Suspicious Listing",
                "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800",
                "Jane Doe",
                3,
                "Fake listing"
            ),
            ReportedProperty(
                "p2",
                "Scam Property",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
                "John Smith",
                5,
                "Misleading information"
            )
        )
    }

    val context = LocalContext.current
    val activity = context as Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Properties", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, AdminActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Reported Properties",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${properties.size} properties flagged",
                                fontSize = 13.sp,
                                color = Gray
                            )
                        }
                    }
                }
            }

            items(properties) { property ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column {

                        AsyncImage(
                            model = property.propertyImage,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = property.propertyTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "By ${property.ownerName}",
                                        fontSize = 13.sp,
                                        color = Gray
                                    )
                                    Text(
                                        text = "Reason: ${property.reportReason}",
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                }

                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${property.reportCount} Reports",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        color = Color(0xFFFF9800),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { /* View details */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View Details")
                                }

                                Button(
                                    onClick = { /* Review */ },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9800)
                                    )
                                ) {
                                    Text("Review")
                                }

                                Button(
                                    onClick = { /* Remove */ },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red
                                    )
                                ) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}