package com.sunshine.appsuite.budget.home.ui.widgets.appointments

import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import java.time.ZonedDateTime

data class HomeAppointmentItem(
    val id: Long,
    val dateTime: ZonedDateTime,
    val type: AppointmentType = AppointmentType.UNKNOWN,
    val customerName: String,
    val status: String? = null,
    val vehicleSummary: String? = null
)
