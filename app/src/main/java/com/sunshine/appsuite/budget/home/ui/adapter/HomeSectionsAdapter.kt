package com.sunshine.appsuite.budget.home.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.home.model.HomeSection
import com.sunshine.appsuite.budget.home.ui.touch.HomeSectionsDragStartListener
import com.sunshine.appsuite.budget.home.ui.touch.HomeSectionsTouchAdapter
import java.util.Collections

class HomeSectionsAdapter(
    private val sections: MutableList<HomeSection>,
    private val dragStartListener: HomeSectionsDragStartListener,
    private val onBindSection: (View, HomeSection) -> Unit
) : RecyclerView.Adapter<HomeSectionsAdapter.SectionVH>(), HomeSectionsTouchAdapter {

    override fun getItemCount(): Int = sections.size

    override fun getItemViewType(position: Int): Int = sections[position].type.layoutRes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionVH {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return SectionVH(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SectionVH, position: Int) {
        val section = sections[position]

        holder.dragHandle?.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dragStartListener.onStartDrag(holder)
                true
            } else {
                false
            }
        }

        onBindSection(holder.itemView, section)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSections(newSections: List<HomeSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in sections.indices || toPosition !in sections.indices) return false
        Collections.swap(sections, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun getCurrentSections(): List<HomeSection> = sections.toList()

    class SectionVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dragHandle: ImageView? = itemView.findViewById(R.id.dragHandle)
    }
}
