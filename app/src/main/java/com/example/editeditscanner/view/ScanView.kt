package com.example.editeditscanner.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.editeditscanner.R
import com.example.editeditscanner.data.BoundingRect
import org.opencv.core.Point

class ScanView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private val line: Paint = Paint()
    private val circleStroke: Paint = Paint()
    private val fill: Paint = Paint()
    private var dynamicBoundingRect: BoundingRect
    private var delayedHandler: Handler? = null
    private var delayedRunnable: Runnable? = null

    init {
        val color = context.getColor(R.color.colorAccent)
        dynamicBoundingRect = BoundingRect()
        val strokeWidth = 8
        line.strokeWidth = strokeWidth.toFloat()
        line.isAntiAlias = true
        line.style = Paint.Style.STROKE
        line.color = color
        val circleStrokeWidth = 6
        circleStroke.strokeWidth = circleStrokeWidth.toFloat()
        circleStroke.isAntiAlias = true
        circleStroke.style = Paint.Style.STROKE
        circleStroke.color = Color.WHITE
        fill.color = context.getColor(R.color.colorAccent)
    }

    private fun drawPin(canvas: Canvas, point: Point) {
        val inner = 12
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), (inner * 2).toFloat(), fill)
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), (inner * 2).toFloat(), circleStroke)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val tl = dynamicBoundingRect.topLeft
        val tr = dynamicBoundingRect.topRight
        val bl = dynamicBoundingRect.bottomLeft
        val br = dynamicBoundingRect.bottomRight
        canvas.drawLine(tl.x.toFloat(), tl.y.toFloat(), tr.x.toFloat(), tr.y.toFloat(), line)
        canvas.drawLine(tr.x.toFloat(), tr.y.toFloat(), br.x.toFloat(), br.y.toFloat(), line)
        canvas.drawLine(br.x.toFloat(), br.y.toFloat(), bl.x.toFloat(), bl.y.toFloat(), line)
        canvas.drawLine(bl.x.toFloat(), bl.y.toFloat(), tl.x.toFloat(), tl.y.toFloat(), line)
        drawPin(canvas, dynamicBoundingRect.topLeft)
        drawPin(canvas, dynamicBoundingRect.topRight)
        drawPin(canvas, dynamicBoundingRect.bottomLeft)
        drawPin(canvas, dynamicBoundingRect.bottomRight)
        drawPin(canvas, dynamicBoundingRect.getTop())
        drawPin(canvas, dynamicBoundingRect.getBottom())
        drawPin(canvas, dynamicBoundingRect.getLeft())
        drawPin(canvas, dynamicBoundingRect.getRight())
    }

    private fun startDelayedHandler() {
        delayedHandler = Handler(Looper.getMainLooper())
        if (delayedRunnable == null) delayedRunnable = Runnable {
            dynamicBoundingRect = BoundingRect(-100.0)
            invalidate()
            removeDelayedHandler()
        }
        delayedHandler?.postDelayed(delayedRunnable!!, 100)
    }

    private fun removeDelayedHandler() {
        delayedHandler?.removeCallbacks(delayedRunnable!!)
        delayedHandler = null
    }

    fun setBoundingRect(boundingRect: BoundingRect?) {
        if (boundingRect != null) {
            if (delayedHandler != null) {
                removeDelayedHandler()
            }
            dynamicBoundingRect = boundingRect
            invalidate()
        } else {
            if (delayedHandler == null) {
                startDelayedHandler()
            }
        }
    }
}