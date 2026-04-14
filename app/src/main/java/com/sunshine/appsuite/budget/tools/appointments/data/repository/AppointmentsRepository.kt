// AppointmentsRepository.kt
package com.sunshine.appsuite.budget.tools.appointments.data.repository

import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.ClientDto
import retrofit2.HttpException
import com.google.gson.Gson
import com.google.gson.JsonParser

class AppointmentsRepository(
    private val api: AppointmentsApi
) {
    private val gson = Gson()

    suspend fun fetchAppointment(id: Long): Result<AppointmentDto> = runCatching {
        val resp = api.getAppointmentRaw(id)
        if (!resp.isSuccessful) throw HttpException(resp)

        val raw = resp.body()?.string().orEmpty()
        if (raw.isBlank()) error("Empty body on getAppointment($id)")

        val root = JsonParser.parseString(raw)
        val node = when {
            root.isJsonObject &&
                    root.asJsonObject.has("data") &&
                    root.asJsonObject.get("data").isJsonObject ->
                root.asJsonObject.getAsJsonObject("data")

            root.isJsonObject -> root.asJsonObject
            else -> error("Unexpected JSON for getAppointment($id): $raw")
        }

        val dto = gson.fromJson(node, AppointmentDto::class.java)
        if (dto.id == null) error("Parsed appointment without id for getAppointment($id)")
        dto
    }


    private suspend fun fetchAppointmentFromList(id: Long): AppointmentDto? {
        val resp = api.getAppointments()

        // No dependemos de cómo sea AppointmentListResponse: lo convertimos a JSON y buscamos array
        val root = JsonParser.parseString(gson.toJson(resp))

        val array = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                when {
                    obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                    obj.has("appointments") && obj.get("appointments").isJsonArray -> obj.getAsJsonArray("appointments")
                    obj.has("results") && obj.get("results").isJsonArray -> obj.getAsJsonArray("results")
                    else -> null
                }
            }
            else -> null
        } ?: return null

        for (el in array) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject

            val elId = runCatching { o.get("id")?.asLong }.getOrNull()
                ?: runCatching { o.get("id")?.asString?.toLong() }.getOrNull()

            if (elId == id) {
                return gson.fromJson(o, AppointmentDto::class.java)
            }
        }
        return null
    }

    suspend fun fetchAppointments(): Result<List<AppointmentDto>> = runCatching {
        api.getAppointments().data
    }

    // =====================
    // EDITAR
    // =====================

    suspend fun updateAppointment(
        id: Long,
        body: Map<String, Any>
    ): Result<AppointmentDto> = runCatching {
        val resp = api.updateAppointment(id, body)
        if (!resp.isSuccessful) throw HttpException(resp)
        fetchAppointment(id).getOrThrow()
    }

    // ======== ESTADOS ========

    /**
     * Confirma la cita en backend.
     *
     * Nota: Algunos endpoints de acciones (confirm/start/complete/cancel) pueden responder
     * sin body útil (204/200 vacío). Para evitar falsos "errores" en UI, aquí sólo validamos
     * el HTTP 2xx. El refresh del detalle/lista se hace aparte.
     */
    suspend fun confirmAppointment(id: Long): Result<Unit> = runCatching {
        val resp = api.confirmAppointment(id)
        if (!resp.isSuccessful) throw HttpException(resp)
        Unit
    }

    suspend fun startAppointment(id: Long): Result<AppointmentDto> = runCatching {
        val resp = api.startAppointment(id)
        if (!resp.isSuccessful) throw HttpException(resp)
        fetchAppointment(id).getOrThrow()
    }

    suspend fun completeAppointment(id: Long): Result<AppointmentDto> = runCatching {
        val resp = api.completeAppointment(id)
        if (!resp.isSuccessful) throw HttpException(resp)
        fetchAppointment(id).getOrThrow()
    }

    suspend fun rescheduleAppointment(
        id: Long,
        body: Map<String, Any>
    ): Result<AppointmentDto> = runCatching {
        val resp = api.rescheduleAppointment(id, body)
        if (!resp.isSuccessful) throw HttpException(resp)
        fetchAppointment(id).getOrThrow()
    }

    suspend fun cancelAppointment(id: Long): Result<AppointmentDto> = runCatching {
        val resp = api.cancelAppointment(id)
        if (!resp.isSuccessful) throw HttpException(resp)
        fetchAppointment(id).getOrThrow()
    }


    suspend fun fetchClients(): Result<List<ClientDto>> = runCatching {
        api.getClients().data
    }
}
