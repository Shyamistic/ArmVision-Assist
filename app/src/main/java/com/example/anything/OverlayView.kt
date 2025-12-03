package com.example.armvisionassist

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val scrimPaint = Paint().apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val scanLinePaint = Paint().apply {
        color = Color.parseColor("#00E676") // Matrix Green
        strokeWidth = 4f
        style = Paint.Style.STROKE
        shader = LinearGradient(0f, 0f, 100f, 0f, Color.TRANSPARENT, Color.GREEN, Shader.TileMode.MIRROR)
        alpha = 150
    }

    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val boxRect = RectF()
    private val cornerRadius = 30f

    // Animation Variables
    private var scanY = 0f
    private var scanDirection = 1 // 1 = down, -1 = up

    init {
        // Start the infinite scanning loop
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            // We calculate Y manually in onDraw to keep it simple
            invalidate()
        }
        animator.start()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val marginX = width * 0.1f
        val boxWidth = width - (2 * marginX)
        val boxHeight = boxWidth * 1.2f // Slightly taller than wide
        val topOffset = height * 0.2f
        boxRect.set(marginX, topOffset, width - marginX, topOffset + boxHeight)

        // Init scan line
        scanY = boxRect.top
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Scrim
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // 2. Clear Hole
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, eraserPaint)

        // 3. Draw High-Tech Brackets
        drawBrackets(canvas)

        // 4. Draw Moving Scan Line
        updateScanLine()
        canvas.drawLine(boxRect.left + 20, scanY, boxRect.right - 20, scanY, scanLinePaint)

        canvas.restoreToCount(layerId)
    }

    private fun updateScanLine() {
        val speed = 10f
        scanY += (speed * scanDirection)
        if (scanY > boxRect.bottom) {
            scanY = boxRect.bottom
            scanDirection = -1
        } else if (scanY < boxRect.top) {
            scanY = boxRect.top
            scanDirection = 1
        }
    }

    private fun drawBrackets(canvas: Canvas) {
        val len = boxRect.width() * 0.15f
        // Top Left
        canvas.drawLine(boxRect.left, boxRect.top, boxRect.left + len, boxRect.top, borderPaint)
        canvas.drawLine(boxRect.left, boxRect.top, boxRect.left, boxRect.top + len, borderPaint)
        // Top Right
        canvas.drawLine(boxRect.right, boxRect.top, boxRect.right - len, boxRect.top, borderPaint)
        canvas.drawLine(boxRect.right, boxRect.top, boxRect.right, boxRect.top + len, borderPaint)
        // Bottom Left
        canvas.drawLine(boxRect.left, boxRect.bottom, boxRect.left + len, boxRect.bottom, borderPaint)
        canvas.drawLine(boxRect.left, boxRect.bottom, boxRect.left, boxRect.bottom - len, borderPaint)
        // Bottom Right
        canvas.drawLine(boxRect.right, boxRect.bottom, boxRect.right - len, boxRect.bottom, borderPaint)
        canvas.drawLine(boxRect.right, boxRect.bottom, boxRect.right, boxRect.bottom - len, borderPaint)
    }

    fun flashSuccess() {
        // Just make the border green for a moment
        borderPaint.color = Color.GREEN
        postDelayed({ borderPaint.color = Color.WHITE; invalidate() }, 500)
        invalidate()
    }
}