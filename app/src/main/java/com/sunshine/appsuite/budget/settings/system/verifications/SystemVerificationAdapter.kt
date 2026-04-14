package com.sunshine.appsuite.budget.settings.system.verifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ItemSystemVerificationBinding

class SystemVerificationAdapter(
    private val onAction: (SystemCheckId, SystemCheckAction?) -> Unit
) : RecyclerView.Adapter<SystemVerificationAdapter.VH>() {

    private val items = mutableListOf<SystemCheckUi>()

    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long = items[position].id.ordinal.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSystemVerificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<SystemCheckUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun update(id: SystemCheckId, transform: (SystemCheckUi) -> SystemCheckUi) {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return
        items[index] = transform(items[index])
        notifyItemChanged(index)
    }

    inner class VH(private val b: ItemSystemVerificationBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: SystemCheckUi) {
            b.tvLabel.text = item.title
            b.tvSubtitle.text = item.subtitle
            b.tvMeta.text = item.meta

            val running = item.state == SystemCheckState.RUNNING
            b.progressStatus.visibility = if (running) View.VISIBLE else View.GONE
            b.ivStatus.visibility = if (running) View.INVISIBLE else View.VISIBLE

            val iconRes = when (item.state) {
                SystemCheckState.OK -> R.drawable.ic_system_check_ok
                SystemCheckState.WARN -> R.drawable.ic_system_check_warn
                SystemCheckState.ERROR -> R.drawable.ic_system_check_error
                else -> R.drawable.ic_system_check_ok
            }
            b.ivStatus.setImageResource(iconRes)

            val tint = when (item.state) {
                SystemCheckState.OK -> R.color.google_green
                SystemCheckState.WARN -> R.color.google_yellow
                SystemCheckState.ERROR -> R.color.google_red
                else -> R.color.google_icons_state
            }
            b.ivStatus.setColorFilter(ContextCompat.getColor(b.root.context, tint))

            val showAction = item.action != null
            b.btnAction.visibility = if (showAction) View.VISIBLE else View.GONE
            b.btnAction.text = item.actionLabel ?: ""
            b.btnAction.setOnClickListener { onAction(item.id, item.action) }
        }
    }
}
