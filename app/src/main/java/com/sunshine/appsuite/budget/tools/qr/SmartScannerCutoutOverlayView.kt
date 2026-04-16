package com.sunshine.appsuite.budget.tools.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SmartScannerCutoutOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var cutoutWidthDp: Float = 632f
    var cutoutHeightDp: Float = 280f
    var cutoutCornerRadiusDp: Float = 24f
    var scrimColor: Int = 0xB3000000.toInt()

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val cutoutRect = RectF()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        scrimPaint.color = scrimColor

        val w = width.toFloat()
        val h = height.toFloat()
        val rectW = dp(cutoutWidthDp)
        val rectH = dp(cutoutHeightDp)
        val left = (w - rectW) / 2f
        val top = (h - rectH) / 2f
        cutoutRect.set(left, top, left + rectW, top + rectH)
        val radius = dp(cutoutCornerRadiusDp)
        val checkpoint = canvas.saveLayer(0f, 0f, w, h, null)

        canvas.drawRect(0f, 0f, w, h, scrimPaint)
        canvas.drawRoundRect(cutoutRect, radius, radius, clearPaint)
        canvas.restoreToCount(checkpoint)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}