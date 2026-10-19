package com.example.gharbato.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** Shared spacing and control sizes for compact, touch-friendly layouts. */
object GharBatoSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 24.dp
    val section = 32.dp
    val screenHorizontal = 20.dp
}

/** One corner family for cards, fields, sheets, and grouped content. */
object GharBatoShapes {
    val small = RoundedCornerShape(10.dp)
    val control = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)

    val material = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = small,
        medium = control,
        large = card,
        extraLarge = large
    )
}

object GharBatoDimensions {
    val touchTarget = 48.dp
    val compactIcon = 18.dp
    val standardIcon = 24.dp
}
