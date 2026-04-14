package com.sunshine.appsuite.budget.home.ui.widgets.appointments

import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import java.time.DayOfWeek
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object HomeAppointmentsUiFormatter {

    private val localeEsMx = Locale("es", "MX")

    // AM/PM en inglés como tus ejemplos (PM)
    private val time12h = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    // AM/PM con cero a la izquierda (03:00 PM)
    private val time12hPadded = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    // Día del mes SIN cero a la izquierda (6 en vez de 06)
    private val dayNum = DateTimeFormatter.ofPattern("d", localeEsMx)
    private val dayNum2 = DateTimeFormatter.ofPattern("dd", localeEsMx)
    private val monthName = DateTimeFormatter.ofPattern("MMMM", localeEsMx)

    /** "Mie, 12 de Enero - 1:00 PM" */
    fun formatDateTimeHeader(zdt: ZonedDateTime): String {
        val dow = dayAbbrevEs3(zdt.dayOfWeek)
        val dNum = zdt.format(dayNum)
        val mName = zdt.format(monthName).replaceFirstChar { it.titlecase(localeEsMx) }
        val t = zdt.format(time12h)
        return "$dow, $dNum de $mName - $t"
    }

    /** Ej: "JUE 05 FEB" (fecha del widget Home) */
    fun formatWidgetDate(zdt: ZonedDateTime): String {
        val dow = dayAbbrevEs3(zdt.dayOfWeek).uppercase(localeEsMx)
        val dNum = zdt.format(dayNum2)
        val mon = monthAbbrevEs3(zdt.month).uppercase(localeEsMx)
        return "$dow $dNum $mon"
    }

    /** Ej: "03:00 PM" */
    fun formatWidgetTime(zdt: ZonedDateTime): String = zdt.format(time12hPadded)

    /** Ej: "Presup." / "Ingreso" / "Entrega" */
    fun formatTypeShort(type: AppointmentType): String = when (type) {
        AppointmentType.PRESUPUESTO -> "Presup."
        AppointmentType.INGRESO -> "Ingreso"
        AppointmentType.ENTREGA -> "Entrega"
        AppointmentType.UNKNOWN -> "Cita"
    }

    private fun monthAbbrevEs3(month: Month): String = when (month) {
        Month.JANUARY -> "Ene"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Abr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Ago"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dic"
    }

    private fun dayAbbrevEs3(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "Lun"
        DayOfWeek.TUESDAY -> "Mar"
        DayOfWeek.WEDNESDAY -> "Mie"
        DayOfWeek.THURSDAY -> "Jue"
        DayOfWeek.FRIDAY -> "Vie"
        DayOfWeek.SATURDAY -> "Sab"
        DayOfWeek.SUNDAY -> "Dom"
    }
}
