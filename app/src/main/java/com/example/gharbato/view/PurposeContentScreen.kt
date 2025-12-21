package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@Composable
fun PurposeContentScreen(
    selectedPurpose: String,
    onPurposeChange: (String) -> Unit,
    selectedPropertyType: String,
    onPropertyTypeChange: (String) -> Unit
) {
    val propertyTypes = listOf("Apartment", "House", "Villa", "Studio")
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ){
        Row {
            Text(
                "What would you like to do?",
                style = TextStyle(
                    color = Color.DarkGray,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 5.dp)
            )
        }

        Column (
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 15.dp)
                .background(Color.White)
        ){
            listOf("Sell", "Rent", "Book").forEachIndexed { index, title ->
                OutlinedCard(
                    onClick = { onPurposeChange(title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPurpose == title) Blue else Color.Transparent,
                        contentColor = if (selectedPurpose == title) Color.White else Color.Black
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(
                                    when(index){
                                        0 -> R.drawable.tag
                                        1 -> R.drawable.home
                                        2 -> R.drawable.calendar
                                        else -> R.drawable.home
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (selectedPurpose == title) Color.White else Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    when(index){
                                        0 -> "Sell Property"
                                        1 -> "Rent Out Property"
                                        2 -> "Short-Term Booking"
                                        else -> ""
                                    },
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    when(index) {
                                        0 -> "List your property for sale"
                                        1 -> "Find tenants for long-term rental"
                                        2 -> "Vacation rental or daily booking"
                                        else -> ""
                                    },
                                    fontSize = 13.sp,
                                    color = if (selectedPurpose == title)
                                        Color.White.copy(alpha = 0.85f)
                                    else
                                        Gray
                                )
                            }
                        }

                        if (selectedPurpose == title) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_check_24),
                                contentDescription = "Selected",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Property Type",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                propertyTypes.forEach { type ->
                    OutlinedCard(
                        onClick = { onPropertyTypeChange(type) },
                        modifier = Modifier
                            .height(55.dp)
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if(selectedPropertyType == type) Blue else Color.Transparent,
                            contentColor = if(selectedPropertyType == type) Color.White else Color.Black
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                            Text(
                                text = type,
                                fontSize = 16.sp,
                                color = if (selectedPropertyType == type)
                                    Color.White
                                else
                                    Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}