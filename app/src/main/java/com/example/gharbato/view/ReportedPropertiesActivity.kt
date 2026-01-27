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
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.SystemBarUtils
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
        ThemePreference.init(this)
        setContent {
            val isDarkMode by ThemePreference.isDarkModeState.collectAsStateWithLifecycle()
            SystemBarUtils.setSystemBarsAppearance(this, isDarkMode)
            val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()

            GharBatoTheme(darkTheme = isDarkMode) {
                ReportedPropertiesScreen(
                uiState = uiState,
                onDeleteProperty = { reportId, propertyId, ownerId, title ->
                    reportViewModel.deleteReportedProperty(
                        reportId = reportId,
                        propertyId = propertyId,
                        ownerId = ownerId,
                        propertyTitle = title
                    )
                },
                onKeepProperty = { reportId ->
                    reportViewModel.keepProperty(reportId)
                },
                onBack = {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish()
                }
            )

            LaunchedEffect(uiState.successMessage) {
                uiState.successMessage?.let {
                    Toast.makeText(this@ReportedPropertiesActivity, it, Toast.LENGTH_SHORT).show()
                    reportViewModel.clearMessages()
                }
            }

            LaunchedEffect(uiState.error) {
                uiState.error?.let {
                    Toast.makeText(this@ReportedPropertiesActivity, it, Toast.LENGTH_SHORT).show()
                    reportViewModel.clearMessages()
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedPropertiesScreen(
    uiState: com.example.gharbato.viewmodel.ReportUiState,
    onDeleteProperty: (String, Int, String, String) -> Unit,
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
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.reportedProperties.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No Reported Properties", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("All listings are in good standing", fontSize = 14.sp, color = Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.reportedProperties) { report ->
                        ReportedPropertyCard(
                            report = report,
                            onDelete = {
                                onDeleteProperty(
                                    report.reportId,
                                    report.propertyId,
                                    report.ownerId,
                                    report.propertyTitle
                                )
                            },
                            onKeep = { onKeepProperty(report.reportId) },
                            onViewDetails = {
                                context.startActivity(
                                    Intent(context, PropertyDetailActivity::class.java).apply {
                                        putExtra("propertyId", report.propertyId)
                                    }
                                )
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

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    if (actionType == "delete") "Delete Property?" else "Keep Property?",
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
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            AsyncImage(
                model = report.propertyImage.ifEmpty { "https://via.placeholder.com/600x400?text=No+Image" },
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )

            Column(Modifier.padding(16.dp)) {
                Text(report.propertyTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("By ${report.ownerName}", fontSize = 13.sp, color = Gray)

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            actionType = "keep"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Keep")
                    }

                    Button(
                        onClick = {
                            actionType = "delete"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}