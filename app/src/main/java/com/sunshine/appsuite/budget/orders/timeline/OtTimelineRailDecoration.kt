package com.sunshine.appsuite.budget.orders.timeline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.budget.R

class OtTimelineRailDecoration(
    context: Context,
    @IdRes private val dotContainerId: Int = R.id.layoutDotContainer,
    private val getActiveIndex: () -> Int,
    strokeWidthDp: Float = 4f,
    @ColorInt activeColor: Int = ContextCompat.getColor(context, R.color.google_green),
    @ColorInt inactiveColor: Int = ContextCompat.getColor(context, R.color.google_line),
) : RecyclerView.ItemDecoration() {

    private val density = context.resources.displayMetrics.density
    private val strokePx = strokeWidthDp * density

    private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = activeColor
    }

    private val paintInactive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = inactiveColor
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val centers = buildCenters(parent)
        if (centers.size < 2) return

        val activeIndex = getActiveIndex()

        // Dibuja segmento por segmento (entre dotCenters)
        for (i in 0 until centers.size - 1) {
            val a = centers[i]
            val b = centers[i + 1]

            // Segmentos arriba del "active" -> verde
            // Segmentos desde el "active" hacia abajo -> gris
            val paint = when {
                activeIndex < 0 -> paintInactive
                a.pos < activeIndex -> paintActive
                else -> paintInactive
            }

            c.drawLine(a.cx, a.cy, b.cx, b.cy, paint)
        }
    }

    private data class Center(val pos: Int, val cx: Float, val cy: Float)

    private fun buildCenters(parent: RecyclerView): MutableList<Center> {
        val list = mutableListOf<Center>()

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (pos == RecyclerView.NO_POSITION) continue

            val anchor = child.findViewById<View>(dotContainerId) ?: continue

            val cx = child.left + anchor.left + anchor.width / 2f
            val cy = child.top + anchor.top + anchor.height / 2f

            list.add(Center(pos, cx, cy))
        }

        list.sortBy { it.pos }
        return list
    }
}
