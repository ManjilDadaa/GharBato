package com.example.gharbato.view

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray

@Composable
fun PurposeContentScreen() {

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedType by remember { mutableStateOf("") }
    val propertyTypes = listOf("Apartment", "House", "Villa", "Studio")

    Column (
//        modifier = Modifier.background(Blue)
    ){
        Row {
            Text(
                "What would you like to do?",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 5.dp)
            )
        }

        Column (
            modifier = Modifier
//                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 15.dp)
                .background(Color.White)
        ){
            listOf("Sell", "Rent", "Book").forEachIndexed { index, title ->
                OutlinedCard(
                    onClick = {selectedIndex = index},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedIndex == index) Blue else Color.Transparent,
                        contentColor = if (selectedIndex == index) Color.White else Color.Black
                    ),
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(
                                    when(index){
                                        0 -> R.drawable.tag
                                        1 -> R.drawable.home
                                        2 -> R.drawable.calendar
                                        else -> 0
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(start = 10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    when(index){
                                        0 -> "Sell Property"
                                        1 -> "Rent Out Property"
                                        2 -> "Short-term Booking"
                                        else -> ""
                                    },
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Text(when(index) {
                                    0 -> "List your property for sell"
                                    1 -> "FInd tenants for long-term rental"
                                    2 -> "Vacation rental for daily booking"
                                    else -> ""
                                },
                                    fontSize = 15.sp,
                                    color = if (selectedIndex == index) Color.White else Gray
                                )
                            }
                        }

                    }

                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text("Property Type",
                style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                propertyTypes.forEachIndexed { index, type ->
                    OutlinedCard(onClick = {selectedType = type},
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if(selectedType == type) Blue else Color.Transparent,
                            contentColor = if(selectedType == type) Color.White else Color.Black
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                            Text(
                                text = type,
                                fontSize = 16.sp,
                                color = if (selectedType == type)
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

@Preview
@Composable
fun PurposeContentScreenPreview(){
    ListingBody()
}