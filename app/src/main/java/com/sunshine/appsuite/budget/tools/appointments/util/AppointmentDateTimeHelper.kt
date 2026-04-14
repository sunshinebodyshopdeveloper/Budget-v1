package com.sunshine.appsuite.budget.tools.appointments.util

import android.widget.EditText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.DayOfWeek

object AppointmentDateTimeHelper {

    // Guardamos la última lista mostrada para mapear position -> LocalTime
    // (más adelante puedes reemplazar esto por un adapter custom)
    var lastSlotsShown: List<LocalTime> = emptyList()

    private val localeMx = Locale("es", "MX")

    // UI: "Viernes, 09 de Enero de 2026" (tú ya manejas ese estilo)
    private val uiDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", localeMx)

    // UI: "2:00 p. m. hrs"
    private val uiTimeFormatter12h = DateTimeFormatter.ofPattern("h:mm a", localeMx)

    // API: "09-01-2026"
    private val apiDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US)

    // API: "14:00"
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun makeAsPickerField(editText: EditText) {
        editText.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            isClickable = true
            isLongClickable = false
        }
    }

    /**
     * MaterialDatePicker regresa millis en UTC (midnight UTC).
     * Convertimos a LocalDate en UTC para evitar “cambio de día” por zona horaria.
     */
    fun millisToLocalDate(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun formatDateUi(date: LocalDate): String = date.format(uiDateFormatter)

    fun formatTimeUi12h(time: LocalTime): String {
        // Normalizamos AM/PM a algo más “MX”: Android suele poner "a. m."/"p. m." con es-MX
        return "${time.format(uiTimeFormatter12h)} hrs"
    }

    fun formatDateApi(date: LocalDate): String = date.format(apiDateFormatter)

    fun formatTimeApi(time: LocalTime): String = time.format(apiTimeFormatter)

    /**
     * Slots base: 08:00–17:00 cada 30 min (incluye 17:00).
     * Cambia stepMinutes si luego quieres 15/10.
     */
    fun buildTimeSlots(
        start: LocalTime = LocalTime.of(8, 0),
        end: LocalTime = LocalTime.of(17, 0),
        stepMinutes: Int = 60
    ): List<LocalTime> {
        val out = mutableListOf<LocalTime>()
        var t = start
        while (!t.isAfter(end)) {
            out.add(t)
            t = t.plusMinutes(stepMinutes.toLong())
        }
        return out
    }

    fun nextBusinessDay(date: LocalDate): LocalDate {
        var d = date
        while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
            d = d.plusDays(1)
        }
        return d
    }

    fun localDateToUtcMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

//    fun buildTimeSlotsHourly(
//        start: LocalTime = LocalTime.of(8, 0),
//        end: LocalTime = LocalTime.of(17, 0)
//    ): List<LocalTime> {
//        val out = mutableListOf<LocalTime>()
//        var t = start.withMinute(0)
//        val endFixed = end.withMinute(0)
//
//        while (!t.isAfter(endFixed)) {
//            out.add(t)
//            t = t.plusHours(1)
//        }
//        return out
//    }
}
