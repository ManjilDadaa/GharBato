package com.example.gharbato

import android.R
import android.graphics.Color
import android.graphics.Outline
import android.media.MediaCodecInfo
import android.os.Bundle
import android.os.Message
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.Blue
import com.example.gharbato.ui.theme.Gray
import java.util.function.IntConsumer



class MessageScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MessageScreen() }
    }
}
@Composable
fun MessageScreen(){
    var searchText by remember { mutableStateOf("") }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(text = "Messages ",
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 27.sp,
                    fontWeight = FontWeight.W400))

            Spacer(modifier = Modifier.height(15.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it},
                modifier = Modifier.fillMaxWidth()
                    .height(51.dp),
                placeholder = { Text("Search messages...",
                    fontSize = 14.sp,
                    color = Gray)},
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_category_default),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = Gray,
                    focusedContainerColor = Gray.copy(alpha = 0.15f),
                    unfocusedContainerColor = Gray.copy(alpha = 0.15f)

                ),

            )
        }
    }
}

@Preview
@Composable
fun Preview4(){
    MessageScreen()
}