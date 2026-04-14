package com.sunshine.appsuite.budget.settings.system.notifications.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.budget.settings.system.notifications.model.NotificationTopic
import com.sunshine.appsuite.databinding.ItemNotificationToggleBinding

data class NotificationToggleUi(
    val topic: NotificationTopic,
    val title: String,
    val enabled: Boolean,
)

class NotificationTogglesAdapter(
    private val onToggle: (NotificationTopic, Boolean) -> Unit
) : ListAdapter<NotificationToggleUi, NotificationTogglesAdapter.VH>(Diff) {

    private var isBinding = false

    fun submit(items: List<NotificationToggleUi>, bindingMode: Boolean) {
        isBinding = bindingMode
        submitList(items)
        isBinding = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationToggleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemNotificationToggleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: NotificationToggleUi) {
            b.switchItem.text = item.title

            // Evitar loops al setear programáticamente
            b.switchItem.setOnCheckedChangeListener(null)
            b.switchItem.isChecked = item.enabled

            b.switchItem.setOnCheckedChangeListener { _, checked ->
                if (!isBinding) onToggle(item.topic, checked)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<NotificationToggleUi>() {
        override fun areItemsTheSame(old: NotificationToggleUi, new: NotificationToggleUi) =
            old.topic == new.topic

        override fun areContentsTheSame(old: NotificationToggleUi, new: NotificationToggleUi) =
            old == new
    }
}
