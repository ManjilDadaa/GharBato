package com.example.gharbato.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object CustomMarkerHelper {

    fun createPriceMarker(context: Context, price: String): BitmapDescriptor {
        val width = 200
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background paint (green bubble)
        val bgPaint = Paint().apply {
            color = Color(0xFF4CAF50).toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Border paint (white border)
        val borderPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        // Text paint (white text)
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        // Draw rounded rectangle with border
        val rect = RectF(10f, 10f, width - 10f, height - 40f)
        canvas.drawRoundRect(rect, 20f, 20f, bgPaint)
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

        // Draw pointer triangle
        val path = android.graphics.Path()
        path.moveTo(width / 2f - 15f, height - 40f)
        path.lineTo(width / 2f + 15f, height - 40f)
        path.lineTo(width / 2f, height - 10f)
        path.close()
        canvas.drawPath(path, bgPaint)

        // Draw text in center
        val textY = (rect.top + rect.bottom) / 2f + 12f
        canvas.drawText(price, width / 2f, textY, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}