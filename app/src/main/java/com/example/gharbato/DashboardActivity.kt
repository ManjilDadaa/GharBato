package com.example.gharbato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gharbato.ui.theme.Green

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DashboardBody()
        }
    }
}

@Composable
fun DashboardBody(){
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, top = 2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable{

                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Location",
                            style = TextStyle(
                                color = Color.Gray.copy(0.9f)
                            ),
                            modifier = Modifier
                                .padding(start = 5.dp)
                        )
                        Spacer(modifier = Modifier
                            .height(3.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_location_on_24),
                                contentDescription = null,
                                tint = Green

                            )
                            Text("Kathmandu, Nepal")
                        }

                    }
                }

                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_notifications_24),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp),
                        tint = Green
                    )
                }

                Spacer(modifier = Modifier
                    .width(6.dp))

                Box(
                    modifier = Modifier
                        .background(color = Color.Gray, shape = CircleShape)

                ){
                    Image(
                        painter = painterResource(R.drawable.baseline_person_24),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable{


                            }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun DashboardPreview(){
    DashboardBody()
}