package com.example.gharbato.view

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Properties", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800))
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
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Report, null, Color(0xFFFF9800), Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Reported Properties", 18.sp, fontWeight = FontWeight.Bold)
                            Text("${properties.size} properties flagged", 13.sp, Gray)
                        }
                    }
                }
            }

            items(properties) { property ->
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column {
                        AsyncImage(
                            property.propertyImage,
                            null,
                            Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(property.propertyTitle, 16.sp, fontWeight = FontWeight.Bold)
                                    Text("By ${property.ownerName}", 13.sp, Gray)
                                    Text("Reason: ${property.reportReason}", 12.sp, Color.Red)
                                }
                                Surface(Color(0xFFFF9800).copy(0.1f), RoundedCornerShape(8.dp)) {
                                    Text(
                                        "${property.reportCount} Reports",
                                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        Color(0xFFFF9800),
                                        12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                OutlinedButton({}, Modifier.weight(1f)) { Text("View Details") }
                                Button({}, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Color(0xFFFF9800))) { Text("Review") }
                                Button({}, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Color.Red)) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}