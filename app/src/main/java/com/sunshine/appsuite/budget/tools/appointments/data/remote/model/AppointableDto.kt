package com.sunshine.appsuite.budget.tools.appointments.data.remote.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AppointableDto(
    val id: Long? = null,

    /**
     * Por si el backend manda 'name' (viejo) o 'client_name' (nuevo).
     */
    @SerializedName(value = "name", alternate = ["client_name"])
    val name: String? = null,

    /** Aguanta string o número sin romper. */
    val phone: JsonElement? = null,

    val email: String? = null,
    val type: String? = null
)
