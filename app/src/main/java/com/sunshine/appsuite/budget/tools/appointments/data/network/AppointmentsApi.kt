// AppointmentsApi.kt
package com.sunshine.appsuite.budget.tools.appointments.data.network

import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentListResponse
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.ClientsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AppointmentsApi {

    // =====================
    // LISTADO / FILTROS / DETALLE
    // =====================

    /**
     * GET /api/v1/appointments
     * Respuesta típica: array de appointments.
     */
    @GET("/api/v1/appointments")
    suspend fun getAppointments(): AppointmentListResponse

    /**
     * GET /api/v1/appointments/{appointmentType}
     *
     * En tu api_collection viene como GET pero con body { "date": ... }.
     * Retrofit no permite body con @GET, por eso usamos @HTTP(hasBody=true).
     *
     * Ejemplo body:
     * mapOf("date" to "2026-01-07")
     */
    @HTTP(method = "GET", path = "/api/v1/appointments/{appointmentType}", hasBody = true)
    suspend fun getAppointmentsByType(
        @Path("appointmentType") appointmentType: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): AppointmentListResponse

    @GET("/api/v1/appointments/{appointment}")
    suspend fun getAppointment(
        @Path("appointment") appointmentId: Long
    ): AppointmentDto

    @GET("/api/v1/appointments/{appointment}")
    suspend fun getAppointmentRaw(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>

    
    /**
     * GET /api/v1/clients
     * Catálogo de clientes (aseguradoras / particulares).
     * Respuesta: { "data": [ {id, name, type, ...}, ... ] }
     */
    @GET("/api/v1/clients")
    suspend fun getClients(): ClientsResponse

// =====================
    // CREAR / ACTUALIZAR / ELIMINAR
    // =====================

    /**
     * POST /api/v1/appointments
     *
     * Body (snake_case):
     * type, status, date, time, comment,
     * client_name, phone, email,
     * brand, model, color, year, plate
     */
    @POST("/api/v1/appointments")
    suspend fun createAppointment(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    /**
     * PUT /api/v1/appointments/{appointment}
     *
     * Body: mismos campos que create + insurance_type (según colección).
     */
    @PUT("/api/v1/appointments/{appointment}")
    suspend fun updateAppointment(
        @Path("appointment") appointmentId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @DELETE("/api/v1/appointments/{appointment}")
    suspend fun deleteAppointment(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>

    // =====================
    // ESTADOS (acciones)
    // =====================

    @POST("/api/v1/appointments/{appointment}/confirm")
    suspend fun confirmAppointment(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>

    @POST("/api/v1/appointments/{appointment}/start")
    suspend fun startAppointment(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>

    @POST("/api/v1/appointments/{appointment}/complete")
    suspend fun completeAppointment(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>

    /**
     * POST /api/v1/appointments/{appointment}/reschedule
     * Body: date, time
     */
    @POST("/api/v1/appointments/{appointment}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("appointment") appointmentId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @POST("/api/v1/appointments/{appointment}/cancel")
    suspend fun cancelAppointment(
        @Path("appointment") appointmentId: Long
    ): Response<ResponseBody>
}
