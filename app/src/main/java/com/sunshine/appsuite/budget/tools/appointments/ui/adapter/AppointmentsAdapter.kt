package com.sunshine.appsuite.budget.tools.appointments.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import com.sunshine.appsuite.budget.tools.appointments.ui.model.AppointmentUi
import com.sunshine.appsuite.databinding.ItemAppointmentsBinding
import java.text.Normalizer

class AppointmentsAdapter(
    private val onDetailsClick: (AppointmentUi) -> Unit
) : ListAdapter<AppointmentUi, AppointmentsAdapter.VH>(Diff) {

    override fun submitList(list: List<AppointmentUi>?) {
        super.submitList(list?.sortedWith(timeAscComparator))
    }

    override fun submitList(list: List<AppointmentUi>?, commitCallback: Runnable?) {
        super.submitList(list?.sortedWith(timeAscComparator), commitCallback)
    }

    private val timeAscComparator = compareBy<AppointmentUi>(
        { timeLabelToMinutes(it.timeLabel) }
    ).thenBy { it.id.toString() } // desempate estable

    private fun timeLabelToMinutes(label: String?): Int {
        if (label.isNullOrBlank()) return Int.MAX_VALUE

        // Busca algo tipo "8:00" o "08:00" aunque venga con basura extra
        val match = Regex("""(\d{1,2})\s*:\s*(\d{2})""").find(label.trim()) ?: return Int.MAX_VALUE
        val h = match.groupValues[1].toIntOrNull() ?: return Int.MAX_VALUE
        val m = match.groupValues[2].toIntOrNull() ?: return Int.MAX_VALUE

        if (h !in 0..23 || m !in 0..59) return Int.MAX_VALUE
        return h * 60 + m
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppointmentsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onDetailsClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemAppointmentsBinding,
        private val onDetailsClick: (AppointmentUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppointmentUi) = with(binding) {
            val ctx = itemView.context
            val placeholder = ctx.getString(R.string.appointments_placeholder_value)

            // ---- Tipo (título + icono)
            tvAppointmentType.text = item.type.displayName
            ivAppointmentIconType.setImageResource(typeToIconRes(item.type))

            // ---- Nombre
            tvAppointmentName.text = item.customerName.trim().takeIf { it.isNotBlank() } ?: placeholder

            // ---- Estado (texto + icono)
            val statusLabel = item.status?.trim().takeIf { !it.isNullOrBlank() }
                ?: ctx.getString(R.string.appointments_status_default)
            tvAppointmentStatus.text = statusLabel
            ivAppointmentIconStatus.setImageResource(statusToIconRes(statusLabel))

            // ---- Comentario
            val commentText = item.comment?.trim().takeIf { !it.isNullOrBlank() }
                ?: ctx.getString(R.string.appointments_comment_empty)
            tvAppointmentComment.text = commentText

            // ---- Vehículo como chips (opcional)
            val v1 = item.vehicleLine1?.trim().takeIf { !it.isNullOrBlank() }
            val v2 = item.vehicleLine2?.trim().takeIf { !it.isNullOrBlank() }

            // De v2 sacamos color y placa si viene como "Rojo • SRS-123-A"
            val parts = v2?.split("•")?.map { it.trim() }.orEmpty()
            val color = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
            val plate = parts.getOrNull(1)?.takeIf { it.isNotBlank() }

            // Chip 1: principal (marca/modelo/año)
            chipVehicleMain.isVisible = v1 != null
            chipVehicleMain.text = v1 ?: ""

            // Chip 2: extra (color)
            chipVehicleExtra.isVisible = color != null
            chipVehicleExtra.text = color ?: ""

            // Chip 3: placa
            chipVehiclePlate.isVisible = plate != null
            chipVehiclePlate.text = plate ?: ""

            // Mostrar el grupo solo si hay al menos 1 chip visible
            cgVehicle.isVisible =
                chipVehicleMain.isVisible || chipVehicleExtra.isVisible || chipVehiclePlate.isVisible


            // ---- Fecha y hora
            tvAppointmentDate.text = item.dayLabel
            tvAppointmentTime.text = item.timeLabel

            // ---- Color del card:
            // 1) si es CANCELADA => appointments_cancel
            // 2) si no => color por tipo
            val isCanceled = isStatusCanceled(statusLabel)
            val bgColorRes = if (isCanceled) R.color.appointments_cancel else item.type.colorRes
            cardAppointments.setCardBackgroundColor(ContextCompat.getColor(ctx, bgColorRes))

            // ---- Click
            cardAppointments.setOnClickListener { onDetailsClick(item) }
        }

        private fun typeToIconRes(type: AppointmentType): Int {
            return when (type) {
                AppointmentType.PRESUPUESTO -> R.drawable.ic_checkbox
                AppointmentType.INGRESO -> R.drawable.ic_input
                AppointmentType.ENTREGA -> R.drawable.ic_done
                AppointmentType.UNKNOWN -> R.drawable.ic_checkbox
            }
        }

        private fun statusToIconRes(status: String): Int {
            val key = normalizeKey(status)
            return when {
                key.contains("confirm") -> R.drawable.ic_check_circle
                key.contains("cancel") -> R.drawable.ic_cancel
                key.contains("pend") -> R.drawable.ic_clock
                else -> R.drawable.ic_clock
            }
        }

        private fun isStatusCanceled(status: String): Boolean {
            val key = normalizeKey(status)
            return key.contains("cancel")
        }

        private fun normalizeKey(text: String): String {
            return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppointmentUi>() {
        override fun areItemsTheSame(oldItem: AppointmentUi, newItem: AppointmentUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AppointmentUi, newItem: AppointmentUi): Boolean =
            oldItem == newItem
    }
}
