package com.example.gharbato.view

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@Composable
fun PurposeContentScreen() {

    var selectedIndex by remember { mutableStateOf(-1) }

    Column {
        Row {
            Text(
                "What would you like to do?",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(start = 10.dp, top = 15.dp, end = 10.dp, bottom = 4.dp)
            )
        }

        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 15.dp)
                .background(Color.White)
        ){
            listOf("Sell", "Rent", "Book").forEachIndexed { index, title ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIndex = index
                        }
                        .size(70.dp),
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
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(when(index) {
                                    0 -> "List your property for sell"
                                    1 -> "FInd tenants for long-term rental"
                                    2 -> "Vacation rental for daily booking"
                                    else -> ""
                                }
                                )
                            }
                        }

                    }

                }
                Spacer(modifier = Modifier.height(10.dp))
            }

        }
    }
}

@Preview
@Composable
fun PurposeContentScreenPreview(){
    ListingBody()
}