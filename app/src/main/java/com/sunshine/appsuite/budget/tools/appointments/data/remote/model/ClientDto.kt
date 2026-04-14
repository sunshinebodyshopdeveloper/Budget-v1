package com.sunshine.appsuite.budget.tools.appointments.data.remote.model

import com.google.gson.annotations.SerializedName

data class ClientDto(
    val id: Long,
    val name: String,
    val type: Int,
    @SerializedName("created_at")
    val createdAt: String? = null
)
