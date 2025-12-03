package com.example.armvisionassist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.View
import java.util.ArrayList

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val lock = Any()
    private val graphics = ArrayList<Graphic>()

    // Matrix Stats
    private var imageWidth = 0
    private var imageHeight = 0
    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f
    private var isImageFlipped = false
    private var needUpdateTransformation = true

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas?)

        fun translateX(x: Float): Float = if (overlay.isImageFlipped) {
            overlay.width - (scale(x) - overlay.postScaleWidthOffset)
        } else {
            scale(x) - overlay.postScaleWidthOffset
        }

        fun translateY(y: Float): Float = scale(y) - overlay.postScaleHeightOffset
        private fun scale(imagePixel: Float): Float = imagePixel * overlay.scaleFactor
    }

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
        postInvalidate()
    }

    fun setImageSourceInfo(width: Int, height: Int, isFlipped: Boolean) {
        synchronized(lock) {
            imageWidth = width
            imageHeight = height
            isImageFlipped = isFlipped
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            updateTransformationIfNeeded()
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) return
        val viewAspectRatio = width.toFloat() / height
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f

        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = ((width.toFloat() / imageAspectRatio) - height) / 2
        } else {
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = ((height.toFloat() * imageAspectRatio) - width) / 2
        }
        needUpdateTransformation = false
    }
}