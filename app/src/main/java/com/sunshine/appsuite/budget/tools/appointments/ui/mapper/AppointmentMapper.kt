package com.sunshine.appsuite.budget.tools.appointments.ui.mapper

import com.google.gson.JsonElement
import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.ui.model.AppointmentUi
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppointmentMapper {

    private val localeEsMx = Locale("es", "MX")

    private val serverFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    )

    fun toUi(dto: AppointmentDto): AppointmentUi {
        val type = AppointmentType.Companion.fromApi(dto.type)

        // Backend nuevo (flat + snake_case) + compat con appointable legacy
        val name = dto.displayClientName?.trim().orEmpty()
        val phone = dto.displayPhone?.trim().orEmpty()
        val email = dto.displayEmail?.trim()?.takeIf { it.isNotBlank() }
        val comment = dto.comment?.trim()?.takeIf { it.isNotBlank() }

        // En tu API vienen separados: date (día) y time (hora) como ISO.
        val dayLabel = formatDay(dto.date)
        val timeLabel: String =
            dto.time
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { if (it.length >= 5) it.substring(0, 5) else it } // "17:00:00" -> "17:00"
                ?: ""
        val dateLabel = formatFullDate(dto.date)

        val (vehicleLine1, vehicleLine2) = buildVehicleLines(dto)

        return AppointmentUi(
            id = dto.id ?: 0L,
            type = type,
            status = dto.status?.trim()?.ifBlank { null },
            comment = comment,
            customerName = name,
            customerPhone = phone,
            dayLabel = dayLabel,
            timeLabel = timeLabel,
            dateLabel = dateLabel,
            customerEmail = email,
            vehicleLine1 = vehicleLine1,
            vehicleLine2 = vehicleLine2
        )
    }

    private fun formatDay(raw: String?): String {
        val zdt = parseToZoned(raw) ?: return "—"
        return zdt.format(DateTimeFormatter.ofPattern("EEEE", localeEsMx))
            .replaceFirstChar { it.titlecase(localeEsMx) }
    }

    private fun formatTime(raw: String?): String {
        val zdt = parseToZoned(raw) ?: return "—"
        return zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)).lowercase()
    }

    private fun formatFullDate(raw: String?): String {
        val zdt = parseToZoned(raw) ?: return "—"

        val dayName = zdt.format(DateTimeFormatter.ofPattern("EEEE", localeEsMx))
            .replaceFirstChar { it.titlecase(localeEsMx) }

        val dayNum = zdt.format(DateTimeFormatter.ofPattern("dd", localeEsMx))

        val month = zdt.format(DateTimeFormatter.ofPattern("MMM", localeEsMx))
            .replace(".", "")
            .replaceFirstChar { it.titlecase(localeEsMx) }

        return "$dayName, $dayNum $month"
    }

    private fun parseToZoned(raw: String?): ZonedDateTime? {
        if (raw.isNullOrBlank()) return null

        runCatching { return ZonedDateTime.parse(raw) }.getOrNull()

        val ldt: LocalDateTime? = serverFormatters.firstNotNullOfOrNull { fmt ->
            runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull()
        }

        return ldt?.atZone(ZoneId.systemDefault())
    }

    private fun buildVehicleLines(dto: AppointmentDto): Pair<String?, String?> {
        val brand = dto.brand?.trim().orEmpty()
        val model = dto.model?.trim().orEmpty()
        val year = dto.yearText?.trim().orEmpty()
        val color = dto.color?.trim().orEmpty()
        val plate = dto.plate?.trim().orEmpty()

        val base = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ")

        val line1 = when {
            base.isNotBlank() && year.isNotBlank() -> "$base ($year)"
            base.isNotBlank() -> base
            else -> null
        }

        val line2 = when {
            color.isNotBlank() && plate.isNotBlank() -> "$color • $plate"
            plate.isNotBlank() -> plate
            color.isNotBlank() -> color
            else -> null
        }

        return line1 to line2
    }

    // (Puede quedarse; no estorba aunque ya no lo usemos aquí)
    private fun JsonElement?.asStringSafe(): String? {
        if (this == null || this.isJsonNull) return null
        return when {
            isJsonPrimitive -> runCatching { asString }.getOrNull()
            else -> null
        }
    }
}
