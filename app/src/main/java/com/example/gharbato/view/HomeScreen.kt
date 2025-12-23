package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.data.model.SampleData.properties
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Purple
import com.example.gharbato.ui.view.PropertyCard
import kotlin.collections.iterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var search by remember { mutableStateOf("") }

    // variables required for multi selection FilterChips
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }
    val filters = mapOf(
        "Trending" to R.drawable.baseline_trending_up_24,
        "Nearby" to R.drawable.outline_location_on_24,
        "Price Range" to R.drawable.baseline_currency_rupee_24,
        "New" to R.drawable.baseline_star_border_purple500_24
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val activity = context as Activity
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, ListingActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = null
                        )
                    }
                },

                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.outline_notifications_24),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FB))
                .padding(padding),
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp, horizontal = 5.dp)
                        .height(48.dp),

                    value = search,
                    onValueChange = { data ->
                        search = data
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Gray.copy(0.2f),
                        unfocusedContainerColor = Color.Gray.copy(0.2f),
                        unfocusedBorderColor = Color.Gray.copy(0.1f),
                        focusedBorderColor = Blue,
                    ),
                    placeholder = {
                        Text(
                            "Search Properties...",
                            style = TextStyle(
                                fontSize = 14.sp
                            )
                        )
                    },

                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.outline_search_24),
                            contentDescription = null
                        )
                    },

                    shape = RoundedCornerShape(10.dp),
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(horizontal = 5.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {

                    for ((filter, icon) in filters) {
                        FilterChip(
                            selected =
                                selectedFilters.contains(filter),
                            onClick = {
                                selectedFilters = if (filter in selectedFilters)
                                    selectedFilters - filter
                                else selectedFilters + filter
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(filter)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple.copy(0.3f)
                            )
                        )
                    }

                }
            }
            items(properties) { property ->
                PropertyCard(property = property, onClick = {})


            }
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview(){
    HomeScreen()
}