package com.sunshine.appsuite.budget.tools.appointments.data.remote.model

import com.google.gson.annotations.SerializedName

data class AppointmentListResponse(
    @SerializedName("data")
    val data: List<AppointmentDto> = emptyList()
)
