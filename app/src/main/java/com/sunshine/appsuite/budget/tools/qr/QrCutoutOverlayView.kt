package com.sunshine.appsuite.budget.tools.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class QrCutoutOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Ajustes rápidos
    var cutoutSizeDp: Float = 260f
    var cutoutCornerRadiusDp: Float = 21f  // ~8% de 260dp (como tu drawable)
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
        // Para que CLEAR funcione bien en la mayoría de dispositivos
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        scrimPaint.color = scrimColor

        val w = width.toFloat()
        val h = height.toFloat()

        val cutoutSize = dp(cutoutSizeDp)

        // Centrado (igual que tu diseño actual)
        val left = (w - cutoutSize) / 2f
        val top = (h - cutoutSize) / 2f
        cutoutRect.set(left, top, left + cutoutSize, top + cutoutSize)

        val radius = dp(cutoutCornerRadiusDp)

        // Capa para poder “perforar”
        val checkpoint = canvas.saveLayer(0f, 0f, w, h, null)

        // Scrim completo
        canvas.drawRect(0f, 0f, w, h, scrimPaint)

        // Cutout redondeado
        canvas.drawRoundRect(cutoutRect, radius, radius, clearPaint)

        canvas.restoreToCount(checkpoint)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}