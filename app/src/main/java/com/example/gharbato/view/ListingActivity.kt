package com.example.gharbato.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.R
import com.example.gharbato.data.repository.PropertyRepo
import com.example.gharbato.data.repository.PropertyRepoImpl
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import com.example.gharbato.viewmodel.ListingViewModel

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
    val listingViewModel = remember { ListingViewModel(PropertyRepoImpl()) }

    var step by rememberSaveable { mutableIntStateOf(1) }
    val showHeader = step == 1

    var listingState by rememberSaveable {mutableStateOf(PropertyListingState()) }

    var showExitDialog by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Exit Listing?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to go back? Your progress won't be saved.",
                    fontSize = 15.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                        activity.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    )
                ) {
                    Text("Yes, Exit")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gray.copy(0.7f)
                    )
                ) {
                    Text("Stay")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Do you want to submit the listing?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    listingViewModel.submitListing(context, listingState)
                }) {
                    Text("Yes, Submit")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold (
        containerColor = Color.White
    ){
            padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
        ){
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 600)
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 600)
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(color = Blue, shape = RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.home),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                fontSize = 15.sp,
                                color = Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    Column {
                        val circleColor by animateColorAsState(
                            targetValue = if (step >= i) Blue else Gray.copy(0.3f),
                            label = "stepColor"
                        )
                        Box(
                            modifier = Modifier
                                .background(color = circleColor, shape = CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$i", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when (i) {
                                1 -> "Purpose"
                                2 -> "Details"
                                3 -> "Photos"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }

                    if (i < 3) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 7.dp)
                                .weight(1f)
                                .height(2.dp)
                                .background(if (step > i) Blue else Gray.copy(0.3f))
                        ) {

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                when (step) {
                    1 -> PurposeContentScreen(
                        selectedPurpose = listingState.selectedPurpose,
                        selectedPropertyType = listingState.selectedPropertyType,
                        onPropertyTypeChange = { newType ->
                            listingState = listingState.copy(selectedPropertyType = newType)
                        },
                        onPurposeChange = { newPurpose ->
                            listingState = listingState.copy(selectedPurpose = newPurpose)
                        }
                    )
                    2 -> DetailsContentScreen(
                        state = listingState,
                        onStateChange = { newState ->
                            listingState = newState
                        }
                    )
                    3 -> PhotosContentScreen(
                        imageCategories = listingState.imageCategories,
                        onCategoriesChange = { newCategories ->
                            listingState = listingState.copy(imageCategories = newCategories)
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
            ) {
                Button(
                    onClick = {
                        val newStep = step - 1
                        if (newStep <= 0) {
                            showExitDialog = true
                        } else {
                            step -= 1
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gray.copy(0.7f)
                    )
                ) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.width(7.dp))

                Button(
                    onClick = {
                        if (step <= 2) {
                            step += 1
                        }
                        else if(step == 3){
                            showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    ),
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Preview
@Composable
fun ListingBodyPreview(){
    ListingBody()
}