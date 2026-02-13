package com.tqmane.filmsim.ui

import android.graphics.Matrix
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
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
                if (new in 0.5f..10f && kotlin.math.abs(d.scaleFactor - 1f) > 0.001f)
                    matrix.postScale(d.scaleFactor, d.scaleFactor, d.focusX, d.focusY)
                matrix.postTranslate(d.focusX - prevFX, d.focusY - prevFY)
                applyTransform()
                prevFX = d.focusX; prevFY = d.focusY
                return true
            }
        })

    private val gestureDetector = GestureDetector(glSurfaceView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean { resetZoom(); return true }
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

    fun resetZoom() { matrix.reset(); applyTransform() }

    /** Apply matrix offset so image centres between top bar and control panel. */
    fun applyVerticalOffset(topBarH: Float, panelH: Float) {
        matrix.postTranslate(0f, (topBarH - panelH) / 2f)
        applyTransform()
    }

    fun syncWatermarkPreview(view: ImageView) {
        if (view.visibility != android.view.View.VISIBLE) return
        matrix.getValues(vals)
        view.scaleX = vals[Matrix.MSCALE_X]; view.scaleY = vals[Matrix.MSCALE_X]
        view.translationX = vals[Matrix.MTRANS_X]; view.translationY = vals[Matrix.MTRANS_Y]
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
