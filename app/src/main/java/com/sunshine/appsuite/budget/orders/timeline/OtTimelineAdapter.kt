package com.sunshine.appsuite.budget.orders.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.R

class OtTimelineAdapter(
    private val ctx: Context
) : RecyclerView.Adapter<OtTimelineAdapter.VH>() {

    private val items = mutableListOf<OtTimelineStepUi>()

    // Para el Decoration: índice “hasta donde va el verde”
    val activeIndex: Int
        get() {
            val current = items.indexOfFirst { it.state == OtTimelineState.CURRENT }
            if (current >= 0) return current
            val lastDone = items.indexOfLast { it.state == OtTimelineState.DONE }
            return lastDone // -1 si no hay DONE
        }

    fun submitList(newItems: List<OtTimelineStepUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qr_ot_timeline_step, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val vDotSmall: View = itemView.findViewById(R.id.vDotSmall)
        private val layoutDotCurrent: View = itemView.findViewById(R.id.layoutDotCurrent)

        private val tvTitle: TextView = itemView.findViewById(R.id.tvStepTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvStepDate)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvStepCaption)

        fun bind(item: OtTimelineStepUi) {
            val isDone = item.state == OtTimelineState.DONE
            val isCurrent = item.state == OtTimelineState.CURRENT
            val isTodo = item.state == OtTimelineState.TODO

            // Dots
            if (isCurrent) {
                layoutDotCurrent.visibility = View.VISIBLE
                vDotSmall.visibility = View.GONE
            } else {
                layoutDotCurrent.visibility = View.GONE
                vDotSmall.visibility = View.VISIBLE
                vDotSmall.setBackgroundResource(
                    if (isDone) R.drawable.bg_ot_timeline_dot_done
                    else R.drawable.bg_ot_timeline_dot_todo
                )
            }

            // Texts
            tvTitle.text = ctx.getString(item.titleRes)

            tvDate.text = item.dateText.orEmpty()
            tvDate.visibility = if (item.dateText.isNullOrBlank()) View.GONE else View.VISIBLE

            val captionRes = item.captionRes
            tvCaption.visibility = if (captionRes == null) View.GONE else View.VISIBLE
            if (captionRes != null) tvCaption.text = ctx.getString(captionRes)

            // “google-ish fade”
            val alpha = when {
                isCurrent -> 1f
                isDone -> 0.25f
                else -> 0.35f
            }
            tvTitle.alpha = alpha
            tvDate.alpha = alpha
            tvCaption.alpha = alpha
        }
    }
}
