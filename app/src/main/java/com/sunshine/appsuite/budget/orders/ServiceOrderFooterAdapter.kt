package com.sunshine.appsuite.budget.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.sunshine.appsuite.budget.R

class ServiceOrdersFooterAdapter(
    private val onRetry: () -> Unit
) : RecyclerView.Adapter<ServiceOrdersFooterAdapter.VH>() {

    enum class State { HIDDEN, LOADING, ERROR }

    private var state: State = State.HIDDEN

    fun showLoading() = setState(State.LOADING)
    fun showError() = setState(State.ERROR)
    fun hide() = setState(State.HIDDEN)

    fun isErrorVisible(): Boolean = state == State.ERROR

    private fun setState(newState: State) {
        if (state == newState) return

        val hadItem = state != State.HIDDEN
        val hasItem = newState != State.HIDDEN
        state = newState

        when {
            !hadItem && hasItem -> notifyItemInserted(0)
            hadItem && !hasItem -> notifyItemRemoved(0)
            hadItem && hasItem  -> notifyItemChanged(0)
        }
    }

    override fun getItemCount(): Int = if (state == State.HIDDEN) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_orders_load_state_footer, parent, false)
        return VH(view, onRetry)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(state)
    }

    class VH(
        itemView: View,
        onRetry: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val progress = itemView.findViewById<CircularProgressIndicator>(R.id.progressFooter)
        private val message = itemView.findViewById<TextView>(R.id.tvFooterMessage)
        private val retry = itemView.findViewById<MaterialButton>(R.id.btnFooterRetry)

        init {
            retry.setOnClickListener { onRetry() }
        }

        fun bind(state: State) {
            when (state) {
                State.LOADING -> {
                    progress.visibility = View.VISIBLE
                    retry.visibility = View.GONE
                    message.setText(R.string.service_orders_loading_more)
                }
                State.ERROR -> {
                    progress.visibility = View.GONE
                    retry.visibility = View.VISIBLE
                    message.setText(R.string.service_orders_toast_generic_error)
                }
                State.HIDDEN -> Unit
            }
        }
    }
}
