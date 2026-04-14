package com.sunshine.appsuite.budget.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.databinding.ItemServiceOrderRowBinding
import com.sunshine.appsuite.budget.orders.data.ServiceOrderListItemDto
import java.util.Locale
import com.squareup.picasso.Picasso
import com.sunshine.appsuite.R

class ServiceOrderAdapter(
    private val onClick: (ServiceOrderListItemDto) -> Unit
) : RecyclerView.Adapter<ServiceOrderAdapter.VH>() {

    private val items = mutableListOf<ServiceOrderListItemDto>()

    fun submit(newItems: List<ServiceOrderListItemDto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun append(more: List<ServiceOrderListItemDto>) {
        if (more.isEmpty()) return
        val start = items.size
        items.addAll(more)
        notifyItemRangeInserted(start, more.size)
    }

    fun getItems(): List<ServiceOrderListItemDto> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemServiceOrderRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(
        private val binding: ItemServiceOrderRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServiceOrderListItemDto) = with(binding) {
            val orderCode = item.code?.trim().orEmpty()

            // 1. Carga de Portada con Picasso
            if (orderCode.isNotBlank()) {
                val imageUrl = "https://pilotosadac.com/appsuite/$orderCode/portada.jpg"
                Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.scrim_gradient_expressive)
                    .error(R.color.google_background_settings)
                    .into(ivServiceOrderCover)
            } else {
                ivServiceOrderCover.setImageResource(R.color.google_background_settings)
            }

            // 2. Información de la Orden y Cliente
            val idText = item.id?.toString()?.takeIf { it.isNotBlank() } ?: "—"
            tvServiceOrderId.text = "OS #$idText"
            tvClientName.text = item.clientName?.trim().orEmpty().ifBlank { "—" }

            // 3. Placa en el Chip (Anteriormente Status)
            val plates = item.vehiclePlates?.trim().orEmpty().uppercase(Locale.getDefault())
            if (plates.isBlank()) {
                chipStatus.visibility = View.GONE
            } else {
                chipStatus.visibility = View.VISIBLE
                chipStatus.text = plates
            }

            // 4. Línea de Vehículo (Marca, Modelo, Tipo, Color)
            val vehicleLine = buildVehicleLine(item)
            tvVehicleLine.text = vehicleLine.ifBlank { "—" }

            // 5. Conteo de OTs
            val counts = buildCountsLine(item)
            if (counts.isBlank()) {
                tvOtCounts.visibility = View.GONE
            } else {
                tvOtCounts.visibility = View.VISIBLE
                tvOtCounts.text = counts
            }

            cardServiceOrder.setOnClickListener { onClick(item) }
        }

        private fun buildVehicleLine(item: ServiceOrderListItemDto): String {
            val locale = Locale.getDefault()

            val brand = item.vehicleBrand?.trim().orEmpty()
            val name = item.vehicleName?.trim().orEmpty()
            val year = (item.vehicleYear ?: 0).takeIf { it > 0 }?.toString().orEmpty()

            val title = listOf(brand, name).filter { it.isNotBlank() }.joinToString(" ")
            val titleWithYear = if (year.isNotBlank()) {
                if (title.isBlank()) year else "$title | $year"
            } else title

            val type = item.vehicleType?.trim().orEmpty().capitalizeFirst(locale)
            val color = item.vehicleColor?.trim().orEmpty().capitalizeFirst(locale)
            // Se excluye 'plates' de aquí ya que ahora va en el chipStatus

            val meta = listOf(type, color).filter { it.isNotBlank() }.joinToString(" • ")

            return listOf(titleWithYear, meta).filter { it.isNotBlank() }.joinToString(" — ")
        }

        private fun buildCountsLine(item: ServiceOrderListItemDto): String {
            val total = item.totalOtCount
            val active = item.activeOtCount

            return when {
                total != null && active != null -> "OT: $active activas • $total total"
                total != null -> "OT: $total total"
                active != null -> "OT: $active activas"
                else -> ""
            }
        }

        private fun String.capitalizeFirst(locale: Locale): String {
            val s = trim()
            if (s.isBlank()) return ""
            val lower = s.lowercase(locale)
            return lower.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
            }
        }
    }
}