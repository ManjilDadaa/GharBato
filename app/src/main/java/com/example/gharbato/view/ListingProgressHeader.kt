package com.example.gharbato.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gharbato.ui.theme.LocalGharBatoActionColors

@Composable
internal fun ListingProgressHeader(
    step: Int,
    isRent: Boolean,
    modifier: Modifier = Modifier
) {
    val labels = if (isRent) listOf("Purpose", "Details", "Photos", "Terms", "Amenities")
        else listOf("Purpose", "Details", "Photos", "Amenities")
    val currentStep = step.coerceIn(1, labels.size)
    val actions = LocalGharBatoActionColors.current
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = (currentStep - 1).toFloat() / (labels.size - 1),
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "listingStepProgress"
    )
    Surface(modifier.fillMaxWidth(), color = colors.background) {
        Column {
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).testTag("listing_progress")
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Listing progress"
                        stateDescription = "Step $currentStep of ${labels.size}"
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    }
            ) {
                val circleSize = 36.dp
                val columnWidth = maxWidth / labels.size
                Canvas(Modifier.fillMaxWidth().height(circleSize)) {
                    val slotWidth = size.width / labels.size
                    val startX = slotWidth / 2f
                    val endX = size.width - slotWidth / 2f
                    val centerY = circleSize.toPx() / 2f
                    drawLine(colors.outlineVariant, Offset(startX, centerY), Offset(endX, centerY),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    if (progress > 0f) {
                        drawLine(actions.container, Offset(startX, centerY),
                            Offset(startX + (endX - startX) * progress, centerY),
                            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    labels.forEachIndexed { index, label ->
                        val reached = currentStep >= index + 1
                        val circleColor by animateColorAsState(
                            if (reached) actions.container else colors.outlineVariant,
                            animationSpec = tween(300), label = "listingStepColor"
                        )
                        Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(circleSize).background(circleColor, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (reached) actions.onContainer else colors.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(label, fontSize = 11.sp, lineHeight = 14.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (currentStep == index + 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (reached) colors.onSurface else colors.onSurfaceVariant)
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant, thickness = 0.5.dp)
        }
    }
}
