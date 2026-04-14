package com.sunshine.appsuite.budget.orders.timeline

import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.orders.data.OrderDto
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale

object OtTimelineMapper {

    fun build(order: OrderDto): List<OtTimelineStepUi> {
        val currentIndex = statusToIndex(order.status)

        return listOf(
            step(
                index = 0,
                titleRes = R.string.qr_scanner_ot_step_assigned,
                dateText = formatApiDate(order.createdAt),
                currentIndex = currentIndex
            ),
            step(
                index = 1,
                titleRes = R.string.qr_scanner_ot_step_valuation,
                dateText = null,
                currentIndex = currentIndex
            ),
            step(
                index = 2,
                titleRes = R.string.qr_scanner_ot_step_parts,
                dateText = null,
                currentIndex = currentIndex
            ),
            step(
                index = 3,
                titleRes = R.string.qr_scanner_ot_step_repair,
                dateText = formatApiDate(order.repairStartedAt),
                currentIndex = currentIndex
            ),
            step(
                index = 4,
                titleRes = R.string.qr_scanner_ot_step_delivered,
                dateText = formatApiDate(order.estimatedDeliveryDate)
                    ?: order.estimatedDeliveryDate
                    ?: "—",
                captionRes = R.string.qr_scanner_ot_estimated_delivery_label,
                currentIndex = currentIndex
            ),
        )
    }

    private fun step(
        index: Int,
        titleRes: Int,
        dateText: String?,
        currentIndex: Int,
        captionRes: Int? = null
    ): OtTimelineStepUi {
        val state = when {
            index < currentIndex -> OtTimelineState.DONE
            index == currentIndex -> OtTimelineState.CURRENT
            else -> OtTimelineState.TODO
        }
        return OtTimelineStepUi(
            titleRes = titleRes,
            dateText = dateText,
            captionRes = captionRes,
            state = state
        )
    }

    private fun statusToIndex(statusRaw: String?): Int {
        val s = normalize(statusRaw)

        return when (s) {
            "assigned", "asignado" -> 0
            "valuation", "valuacion" -> 1
            "parts", "refacciones" -> 2
            "repair", "reparacion" -> 3
            "delivered", "entregado", "entrega" -> 4
            else -> 0
        }
    }

    private fun normalize(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return ""
        val noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents.lowercase(Locale.getDefault()).trim()
    }

    private fun formatApiDate(value: String?): String? {
        val v = value?.trim().orEmpty()
        if (v.isBlank()) return null

        // API: "23-01-2026"
        val inFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val outFmt = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))

        return runCatching {
            val date = inFmt.parse(v) ?: return null
            outFmt.format(date).replace(".", "")
        }.getOrNull()
    }
}
