package com.sunshine.appsuite.budget.tools.appointments

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sunshine.appsuite.R
import java.util.Locale

enum class AppointmentType(
    val displayName: String,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int
) {
    PRESUPUESTO("Presupuesto", R.color.appointments_presupuesto, R.drawable.ic_checkbox),
    INGRESO("Ingreso", R.color.appointments_ingreso, R.drawable.ic_input),
    ENTREGA("Entrega", R.color.appointments_entrega, R.drawable.ic_done),
    UNKNOWN("Cita", R.color.google_white, R.drawable.ic_clock);

    companion object {
        fun fromApi(raw: String?): AppointmentType {
            val v = raw
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.replace("á", "a")
                ?.replace("é", "e")
                ?.replace("í", "i")
                ?.replace("ó", "o")
                ?.replace("ú", "u")
                ?: return UNKNOWN

            return when {
                v.contains("presu") -> PRESUPUESTO
                v.contains("ingre") -> INGRESO
                v.contains("entre") -> ENTREGA
                else -> UNKNOWN
            }
        }
    }
}
