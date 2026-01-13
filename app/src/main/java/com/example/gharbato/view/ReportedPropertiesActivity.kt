package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gharbato.model.ReportedProperty
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.LightBlue
import com.example.gharbato.viewmodel.ReportViewModel
import com.example.gharbato.viewmodel.ReportViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class ReportedPropertiesActivity : ComponentActivity() {

    private val reportViewModel: ReportViewModel by viewModels {
        ReportViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()

            ReportedPropertiesScreen(
                uiState = uiState,
                onDeleteProperty = { reportId, propertyId ->
                    reportViewModel.deleteReportedProperty(reportId, propertyId)
                },
                onKeepProperty = { reportId ->
                    reportViewModel.keepProperty(reportId)
                },
                onBack = {
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )

            // Show success/error messages
            LaunchedEffect(uiState.successMessage) {
                uiState.successMessage?.let { message ->
                    Toast.makeText(this@ReportedPropertiesActivity, message, Toast.LENGTH_SHORT).show()
                    reportViewModel.clearMessages()
                }
            }

            LaunchedEffect(uiState.error) {
                uiState.error?.let { error ->
                    Toast.makeText(this@ReportedPropertiesActivity, error, Toast.LENGTH_SHORT).show()
                    reportViewModel.clearMessages()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedPropertiesScreen(
    uiState: com.example.gharbato.viewmodel.ReportUiState,
    onDeleteProperty: (String, Int) -> Unit,
    onKeepProperty: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Properties", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBlue)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.reportedProperties.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No Reports",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Reported Properties",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All listings are in good standing",
                        fontSize = 14.sp,
                        color = Gray
                    )
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
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
                                        text = "${uiState.reportedProperties.size} properties flagged",
                                        fontSize = 13.sp,
                                        color = Gray
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.reportedProperties) { report ->
                        ReportedPropertyCard(
                            report = report,
                            onDelete = { onDeleteProperty(report.reportId, report.propertyId) },
                            onKeep = { onKeepProperty(report.reportId) },
                            onViewDetails = {
                                // Navigate to property details
                                val intent = Intent(context, PropertyDetailActivity::class.java).apply {
                                    putExtra("propertyId", report.propertyId)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportedPropertyCard(
    report: ReportedProperty,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onViewDetails: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = if (actionType == "delete") "Delete Property?" else "Keep Property?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (actionType == "delete")
                        "This will remove the property from all listings. This action cannot be undone."
                    else
                        "This will dismiss the report and keep the property visible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionType == "delete") onDelete() else onKeep()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "delete") Color.Red else Color(0xFF4CAF50)
                    )
                ) {
                    Text(if (actionType == "delete") "Delete" else "Keep")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Property Image
            AsyncImage(
                model = report.propertyImage.ifEmpty { "https://via.placeholder.com/600x400?text=No+Image" },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Property Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.propertyTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "By ${report.ownerName}",
                                fontSize = 13.sp,
                                color = Gray
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reported",
                                color = Color(0xFFFF9800),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Report Details
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reason:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = formatDate(report.reportedAt),
                                fontSize = 11.sp,
                                color = Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.reportReason,
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )

                        if (report.reportDetails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Details:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = report.reportDetails,
                                fontSize = 13.sp,
                                color = Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reported by: ${report.reportedByName}",
                            fontSize = 11.sp,
                            color = Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons - Professional Layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View Details Button (Full Width)
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LightBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Property Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Keep and Remove Buttons (Equal Width)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                actionType = "keep"
                                showConfirmDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keep",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                actionType = "delete"
                                showConfirmDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
