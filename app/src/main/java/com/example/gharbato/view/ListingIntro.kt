package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gharbato.R
import com.example.gharbato.ui.theme.LocalGharBatoActionColors

/** Intro opacity follows the page's scroll; the item keeps its place above sticky progress. */
@Composable
internal fun ListingIntro(opacity: Float, modifier: Modifier = Modifier) {
    val actions = LocalGharBatoActionColors.current
    Column(
        modifier = modifier.fillMaxWidth().testTag("listing_intro")
            .graphicsLayer { alpha = opacity }
            .semantics { if (opacity == 0f) hideFromAccessibility() }
            .padding(horizontal = 8.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).background(actions.container, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.home), contentDescription = "Property icon",
                tint = actions.onContainer, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("List Your Property", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Reach thousands of potential buyers and renters",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}
