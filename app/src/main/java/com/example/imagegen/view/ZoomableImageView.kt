package com.example.imagegen.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

/**
 * 支持双指缩放、双击放大、单指拖动的图片控件
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    
    private val matrix = Matrix()
    private var mode = NONE
    private val startPoint = PointF()
    private val startMatrix = Matrix()
    private var minScale = 1f
    private val maxScale = 4f
    private var originalWidth = 0
    private var originalHeight = 0
    
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    init {
        scaleType = ScaleType.MATRIX
    }
    
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null) {
            originalWidth = drawable.intrinsicWidth
            originalHeight = drawable.intrinsicHeight
            fitCenter()
        }
    }
    
    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        if (bm != null) {
            originalWidth = bm.width
            originalHeight = bm.height
            fitCenter()
        }
    }
    
    private fun fitCenter() {
        if (originalWidth <= 0 || originalHeight <= 0) return
        post {
            if (width <= 0 || height <= 0) return@post
            val scaleX = width.toFloat() / originalWidth
            val scaleY = height.toFloat() / originalHeight
            val scale = min(scaleX, scaleY)
            minScale = scale
            matrix.reset()
            matrix.setScale(scale, scale)
            matrix.postTranslate(
                (width - originalWidth * scale) / 2f,
                (height - originalHeight * scale) / 2f
            )
            imageMatrix = matrix
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = DRAG
                startMatrix.set(matrix)
                startPoint.set(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(startMatrix)
                    matrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                    imageMatrix = matrix
                }
            }
        }
        return true
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            startMatrix.set(matrix)
            startPoint.set(detector.focusX, detector.focusY)
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scale = detector.scaleFactor
            val currentScale = getCurrentScale()
            if (currentScale * scale < minScale) {
                scale = minScale / currentScale
            }
            if (currentScale * scale > maxScale) {
                scale = maxScale / currentScale
            }
            matrix.postScale(scale, scale, startPoint.x, startPoint.y)
            imageMatrix = matrix
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val currentScale = getCurrentScale()
            val targetScale = if (currentScale > minScale * 1.5f) minScale else minScale * 2.5f
            val scale = targetScale / currentScale
            matrix.postScale(scale, scale, e.x, e.y)
            imageMatrix = matrix
            return true
        }
    }
    
    private fun getCurrentScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }
    
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
