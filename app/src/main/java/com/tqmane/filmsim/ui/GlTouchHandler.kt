package com.tqmane.filmsim.ui

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import com.tqmane.filmsim.gl.FilmSimRenderer

/**
 * Encapsulates pinch-to-zoom, pan, double-tap-reset, single-tap (immersive toggle),
 * and long-press (before/after) gesture handling for the GL preview surface.
 */
class GlTouchHandler(
    private val glSurfaceView: GLSurfaceView,
    private val renderer: FilmSimRenderer,
    private val onSingleTap: () -> Unit,
    private val onLongPressStart: () -> Unit,
    private val onLongPressEnd: () -> Unit
) {

    private val matrix = Matrix()
    private val vals = FloatArray(9)
    private var lastX = 0f
    private var lastY = 0f
    private var activePtr = MotionEvent.INVALID_POINTER_ID
    private var initialOffsetY = 0f
    private var initialZoom = 1f

    // Stored image dimensions for zoom recalculation on immersive change
    private var storedImageWidth: Int = 0
    private var storedImageHeight: Int = 0

    // Smooth animation for reset / initial positioning
    private var resetAnimator: ValueAnimator? = null
    private var isFirstAppearance: Boolean = true

    private val scaleDetector = ScaleGestureDetector(glSurfaceView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var prevFX = 0f; private var prevFY = 0f
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                prevFX = d.focusX; prevFY = d.focusY; return true
            }
            override fun onScale(d: ScaleGestureDetector): Boolean {
                matrix.getValues(vals)
                val cur = vals[Matrix.MSCALE_X]
                val new = cur * d.scaleFactor
                if (new in 0.1f..20f && kotlin.math.abs(d.scaleFactor - 1f) > 0.001f)
                    matrix.postScale(d.scaleFactor, d.scaleFactor, d.focusX, d.focusY)
                matrix.postTranslate(d.focusX - prevFX, d.focusY - prevFY)
                applyTransform()
                prevFX = d.focusX; prevFY = d.focusY
                return true
            }
        })

    private val gestureDetector = GestureDetector(glSurfaceView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean { animateToReset(); return true }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { onSingleTap(); return true }
            override fun onLongPress(e: MotionEvent) { onLongPressStart() }
        })

    fun install() {
        glSurfaceView.pivotX = 0f; glSurfaceView.pivotY = 0f
        glSurfaceView.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev); gestureDetector.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastX = ev.x; lastY = ev.y; activePtr = ev.getPointerId(0) }
                MotionEvent.ACTION_POINTER_DOWN -> activePtr = MotionEvent.INVALID_POINTER_ID
                MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && activePtr != MotionEvent.INVALID_POINTER_ID && ev.pointerCount == 1) {
                    val pi = ev.findPointerIndex(activePtr)
                    if (pi >= 0) { matrix.postTranslate(ev.getX(pi) - lastX, ev.getY(pi) - lastY); applyTransform(); lastX = ev.getX(pi); lastY = ev.getY(pi) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { activePtr = MotionEvent.INVALID_POINTER_ID; onLongPressEnd() }
                MotionEvent.ACTION_POINTER_UP -> {
                    val ri = if (ev.actionIndex == 0) 1 else 0
                    if (ri < ev.pointerCount) { lastX = ev.getX(ri); lastY = ev.getY(ri); activePtr = ev.getPointerId(ri) }
                }
            }; true
        }
    }

    /**
     * Animate smoothly to the reset position (initial zoom + offset).
     * Uses a polished decelerate animation.
     */
    private fun animateToReset(durationMs: Long = 400) {
        resetAnimator?.cancel()

        // Capture current state
        matrix.getValues(vals)
        val fromScale = vals[Matrix.MSCALE_X]
        val fromTx = vals[Matrix.MTRANS_X]
        val fromTy = vals[Matrix.MTRANS_Y]

        // Calculate target state
        val w = glSurfaceView.width.toFloat()
        val h = glSurfaceView.height.toFloat()
        val targetScale = initialZoom
        val targetTx = if (w > 0f) w / 2f * (1f - targetScale) else 0f
        val targetTy = if (h > 0f) h / 2f * (1f - targetScale) + initialOffsetY else initialOffsetY

        resetAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float

                val curScale = fromScale + (targetScale - fromScale) * t
                val curTx = fromTx + (targetTx - fromTx) * t
                val curTy = fromTy + (targetTy - fromTy) * t

                matrix.setScale(curScale, curScale)
                matrix.postTranslate(curTx, curTy)
                applyTransform()
            }
            start()
        }
    }

    /**
     * Smooth entrance animation for the first image appearance.
     * Starts from a slightly zoomed-out + lower position and animates to the correct bounds.
     */
    private fun animateToInitialPosition() {
        resetAnimator?.cancel()

        val w = glSurfaceView.width.toFloat()
        val h = glSurfaceView.height.toFloat()
        if (w <= 0f || h <= 0f) { resetZoom(); return }

        // Start state: slightly smaller and shifted down for a gentle entrance
        val fromScale = initialZoom * 0.92f
        val fromTx = w / 2f * (1f - fromScale)
        val fromTy = h / 2f * (1f - fromScale) + initialOffsetY + h * 0.02f

        // Target state
        val targetScale = initialZoom
        val targetTx = w / 2f * (1f - targetScale)
        val targetTy = h / 2f * (1f - targetScale) + initialOffsetY

        // Set starting position immediately
        matrix.setScale(fromScale, fromScale)
        matrix.postTranslate(fromTx, fromTy)
        applyTransform()

        resetAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float

                val curScale = fromScale + (targetScale - fromScale) * t
                val curTx = fromTx + (targetTx - fromTx) * t
                val curTy = fromTy + (targetTy - fromTy) * t

                matrix.setScale(curScale, curScale)
                matrix.postTranslate(curTx, curTy)
                applyTransform()
            }
            start()
        }
    }

    fun resetZoom() {
        resetAnimator?.cancel()
        matrix.reset()
        val w = glSurfaceView.width.toFloat()
        val h = glSurfaceView.height.toFloat()
        if (w > 0f && h > 0f) {
            matrix.postScale(initialZoom, initialZoom, w / 2f, h / 2f)
        } else {
            matrix.postScale(initialZoom, initialZoom)
        }
        if (initialOffsetY != 0f) {
            matrix.postTranslate(0f, initialOffsetY)
        }
        applyTransform()
    }

    /**
     * Calculate the zoom level required to fit the stored image within the available area.
     * Factors in the shader's automatic aspect-ratio scaling and applies a breathing margin.
     */
    private fun calculateZoom(topBarH: Float, panelH: Float): Float {
        val viewW = glSurfaceView.width.toFloat()
        val viewH = glSurfaceView.height.toFloat()
        if (storedImageWidth <= 0 || storedImageHeight <= 0 || viewW <= 0f || viewH <= 0f) return 1f
        val availableH = kotlin.math.max(viewH - topBarH - panelH, viewH * 0.15f)
        val imgRatio = storedImageWidth.toFloat() / storedImageHeight.toFloat()
        val viewRatio = viewW / viewH
        var shaderScaleY = 1f
        if (imgRatio > viewRatio) shaderScaleY = viewRatio / imgRatio
        val baseScreenHeightOccupied = viewH * shaderScaleY
        val breathingMargin = if (baseScreenHeightOccupied > availableH * 0.9f) 0.96f else 0.98f
        return if (baseScreenHeightOccupied > availableH) {
            (availableH / baseScreenHeightOccupied) * breathingMargin
        } else 1f
    }

    /** Apply matrix offset and scale so image fits between top bar and control panel beautifully. */
    fun updateInitialBounds(imageWidth: Int, imageHeight: Int, topBarH: Float, panelH: Float) {
        val viewW = glSurfaceView.width.toFloat()
        val viewH = glSurfaceView.height.toFloat()
        if (viewW <= 0f || viewH <= 0f || imageWidth <= 0 || imageHeight <= 0) return

        storedImageWidth = imageWidth
        storedImageHeight = imageHeight

        val availableH = kotlin.math.max(viewH - topBarH - panelH, viewH * 0.15f)
        initialZoom = calculateZoom(topBarH, panelH)

        // Center the image vertically in the available area between UI elements
        val availableCenterY = topBarH + availableH / 2f
        val screenCenterY = viewH / 2f
        initialOffsetY = availableCenterY - screenCenterY

        // Use smooth entrance animation on first load; snap on subsequent updates
        if (isFirstAppearance) {
            isFirstAppearance = false
            animateToInitialPosition()
        } else {
            resetZoom()
        }
    }

    /**
     * Re-center the preview when immersive mode changes.
     * Smoothly adjusts both offset and zoom to account for changed top/bottom UI heights.
     * Uses a longer animation for a polished feel during mode transitions.
     */
    fun updateForImmersiveChange(topBarH: Float, panelH: Float) {
        val viewW = glSurfaceView.width.toFloat()
        val viewH = glSurfaceView.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val availableH = kotlin.math.max(viewH - topBarH - panelH, viewH * 0.15f)
        val availableCenterY = topBarH + availableH / 2f
        val screenCenterY = viewH / 2f
        val newOffsetY = availableCenterY - screenCenterY
        val newZoom = calculateZoom(topBarH, panelH)

        val offsetChanged = kotlin.math.abs(newOffsetY - initialOffsetY) > 1f
        val zoomChanged = kotlin.math.abs(newZoom - initialZoom) > 0.005f

        if (offsetChanged || zoomChanged) {
            initialOffsetY = newOffsetY
            initialZoom = newZoom
            // Animation duration slightly longer than UI slide to sync with panel transitions
            animateToReset(durationMs = 420)
        }
    }

    private fun applyTransform() {
        matrix.getValues(vals)
        val s = vals[Matrix.MSCALE_X]; val tx = vals[Matrix.MTRANS_X]; val ty = vals[Matrix.MTRANS_Y]
        val w = glSurfaceView.width.toFloat(); val h = glSurfaceView.height.toFloat()
        if (w > 0 && h > 0) {
            val dx = tx + w * s / 2f - w / 2f; val dy = ty + h * s / 2f - h / 2f
            val gx = (dx / w) * 2f; val gy = -(dy / h) * 2f
            glSurfaceView.queueEvent { renderer.updateTransform(s, gx, gy); glSurfaceView.requestRender() }
        }
    }
}
