package com.sunshine.appsuite.budget.tools.appointments.ui.model

import com.sunshine.appsuite.budget.tools.appointments.AppointmentType

data class AppointmentUi(
    val id: Long,
    val type: AppointmentType,
    val status: String?,
    /** Comentario capturado en la cita (puede venir vacío). */
    val comment: String? = null,
    val customerName: String,
    val customerPhone: String,
    val dayLabel: String,   // "Lunes"
    val timeLabel: String,  // "10:30 am"

    /** Formato pensado para el detalle (ej. "Martes, 06 Ene"). */
    val dateLabel: String = dayLabel,

    /** Puede venir vacío en algunos registros. */
    val customerEmail: String? = null,

    /** Vehículo (puede venir vacío). */
    val vehicleLine1: String? = null, // ej: "Nissan Versa (2018)"
    val vehicleLine2: String? = null  // ej: "Rojo • ABC-123"
)
