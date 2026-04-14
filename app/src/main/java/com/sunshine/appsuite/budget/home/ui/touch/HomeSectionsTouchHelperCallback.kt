package com.sunshine.appsuite.budget.home.ui.touch

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.budget.home.ui.adapter.HomeSectionsAdapter

class HomeSectionsTouchHelperCallback(
    private val adapter: HomeSectionsAdapter,
    private val onDragFinished: () -> Unit
) : ItemTouchHelper.Callback() {

    private var dragging = false

    override fun isLongPressDragEnabled(): Boolean = false
    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return adapter.onItemMove(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            dragging = true
            viewHolder.itemView.apply {
                alpha = 0.8f
                scaleX = 1.02f
                scaleY = 1.02f
                elevation = 12f
            }
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && dragging) {
            dragging = false
            onDragFinished()
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        viewHolder.itemView.apply {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            elevation = 0f
        }
    }
}
