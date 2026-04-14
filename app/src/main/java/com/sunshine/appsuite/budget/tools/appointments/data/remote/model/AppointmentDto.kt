package com.sunshine.appsuite.budget.tools.appointments.data.remote.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Mapea la respuesta real del backend (snake_case) que viste en Postman.
 * Ejemplo de campos: client_name, created_at, updated_at, etc.
 */
data class AppointmentDto(
    val id: Long? = null,
    val type: String? = null,
    val status: String? = null,

    /** Ej: "2026-01-07T06:00:00.000000Z" */
    val date: String? = null,

    /** En tu API también viene como string ISO (según Postman). */
    val time: String? = null,

    val comment: String? = null,

    @SerializedName("client_name")
    val clientName: String? = null,

    /** Puede venir como string o número, por eso JsonElement. */
    val phone: JsonElement? = null,

    val email: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,

    /** Puede venir como número o string. */
    val year: JsonElement? = null,

    val plate: String? = null,

    @SerializedName("client_type")
    val clientType: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    /** Legacy (si algún endpoint viejo aún lo manda). */
    val appointable: AppointableDto? = null
) {
    val phoneText: String?
        get() = phone?.takeIf { !it.isJsonNull }?.let {
            if (it.isJsonPrimitive) it.asJsonPrimitive.asString else it.toString()
        }

    val yearText: String?
        get() = year?.takeIf { !it.isJsonNull }?.let {
            if (it.isJsonPrimitive) it.asJsonPrimitive.asString else it.toString()
        }

    /**
     * Helpers de compatibilidad: si tu UI todavía lee 'appointable',
     * aquí damos fallback a los campos nuevos (flat) del backend.
     */
    val displayClientName: String?
        get() = clientName ?: appointable?.name

    val displayEmail: String?
        get() = email ?: appointable?.email

    val displayPhone: String?
        get() = phoneText ?: appointable?.phone?.takeIf { !it.isJsonNull }?.let {
            if (it.isJsonPrimitive) it.asJsonPrimitive.asString else it.toString()
        }
}
