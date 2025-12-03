package com.example.armvisionassist

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.text.Text

class TextGraphic(
    overlay: GraphicOverlay,
    private val element: Text.Element
) : GraphicOverlay.Graphic(overlay) {

    private val rectPaint = Paint().apply {
        color = determineColor(element.text)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private fun determineColor(text: String): Int {
        val lower = text.lowercase()
        return when {
            lower.contains("$") || lower.matches(Regex(".*\\d+.*")) -> Color.GREEN // Financial
            lower.contains("danger") || lower.contains("warning") -> Color.RED     // Safety
            lower.contains("@") || lower.contains("http") -> Color.CYAN            // Links
            else -> Color.WHITE
        }
    }

    override fun draw(canvas: Canvas?) {
        val elementBox = element.boundingBox ?: return

        // Map ML Kit coordinates to View coordinates
        val rect = RectF(elementBox)
        val left = translateX(rect.left)
        val top = translateY(rect.top)
        val right = translateX(rect.right)
        val bottom = translateY(rect.bottom)

        canvas?.drawRect(left, top, right, bottom, rectPaint)
    }
}