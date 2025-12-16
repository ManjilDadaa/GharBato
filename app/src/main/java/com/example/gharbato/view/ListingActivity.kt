package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.view.ui.theme.GharBatoTheme

class ListingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListingBody()
        }
    }
}

@Composable
fun ListingBody(){

    val context = LocalContext.current
    val activity = context as Activity

    var step by remember { mutableStateOf(1) }

    Scaffold {
        padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ){
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ){
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color = Blue, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,

                    ){
                    Icon(
                        painter = painterResource(R.drawable.home),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ){
                Text(
                    "List Your Property",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    "Reach thousands of potential buyers and renters",
                    style = TextStyle(
//                    fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for(i in 1..3){
                    Column {
                        val circleColor by animateColorAsState(
                            targetValue = if(step >= i) Blue else Gray.copy(0.3f), label = "stepColor"
                        )
                        Box(
                            modifier = Modifier
                                .background(color = circleColor , shape = CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ){
                            Text("$i", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when(i){
                                1 -> "Purpose"
                                2 -> "Details"
                                3 -> "Photos"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }

                    if( i < 3){
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 7.dp)
                                .weight(1f)
                                .height(2.dp)
                                .background(if(step > i) Blue else Gray.copy(0.3f))
                        ){

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp)
                    .height(550.dp)
            ) {
                Card (
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .shadow(8.dp)
                        .fillMaxSize()

                ){
                    Box(){
                        when (step){
                            1 -> PurposeContentScreen()
                            2 -> DetailsContentScreen()
                            3 -> PhotosContentScreen()
                        }
                    }
                }

            }
            Row (
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ){
                OutlinedButton(onClick = {
                    val newStep = step - 1
                    if ( newStep<= 0) {
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    } else {step -= 1}
                }) {
                    Text("Back")
                }
                OutlinedButton(onClick = {
                    step += 1
                }) {
                    Text("Next")
                }
            }

        }
    }
}
@Composable
fun PurposeContentScreen() {
    Text("This is Step 1 content")
}

@Composable
fun DetailsContentScreen() {
    Text("This is Step 2 content")
}

@Composable
fun PhotosContentScreen() {
    Text("This is Step 3 content")
}


@Preview
@Composable
fun ListingBodyPreview(){
    ListingBody()
}