package com.example.gharbato.view

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gharbato.view.ui.theme.LightBlue
import com.example.gharbato.view.ui.theme.LightGreen
import com.example.gharbato.view.ui.theme.ReportedRed
import com.example.gharbato.viewmodel.ReportViewModel
import com.example.gharbato.viewmodel.ReportViewModelFactory
import com.example.gharbato.repository.PendingPropertiesRepoImpl
import com.example.gharbato.viewmodel.PendingPropertiesViewModel
import com.example.gharbato.viewmodel.PendingPropertiesViewModelFactory

@Composable
fun AdminHomeScreen() {
    val context = LocalContext.current

    // ViewModels for real-time data
    val reportViewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory()
    )

    val pendingViewModel: PendingPropertiesViewModel = viewModel(
        factory = PendingPropertiesViewModelFactory()
    )

    val reportUiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val pendingUiState by pendingViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Pending Listings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, PendingListingsActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = LightGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Pending Listings",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    if (pendingUiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Text(
                            "${pendingUiState.properties.size}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp
                            )
                        )
                    }

                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Reported Listings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, ReportedPropertiesActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = LightBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Reported Listings",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    if (reportUiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Text(
                            "${reportUiState.reportedProperties.size}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp
                            )
                        )
                    }

                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Reported Users Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(15.dp)
                    .clickable {
                        val intent = Intent(context, ReportedUsersActivity::class.java)
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = ReportedRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Reported Users",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        "0",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 45.sp
                        )
                    )
                    Text(
                        "Tap to review",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}