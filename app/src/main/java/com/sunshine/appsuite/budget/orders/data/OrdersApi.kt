package com.sunshine.appsuite.budget.orders.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OrdersApi {
    @GET("api/v1/service-orders/{service_order}")
    suspend fun getOrder(
        @Path("service_order") orderCode: String
    ): OrderResponse

    @GET("api/v1/service-orders")
    suspend fun searchOrders(
        @Query("search") query: String
    ): ServiceOrdersResponse
}

data class OrderResponse(
    @SerializedName("data") val data: OrderDto,
    @SerializedName("insurance_claim") val insuranceClaim: InsuranceClaimDto?
)

data class OrderDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("code") val code: String?,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_phone") val contactPhone: String?,

    // Tiempos y Fechas
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("repair_started_at") val repairStartedAt: String?,
    @SerializedName("estimated_delivery_date") val estimatedDeliveryDate: String?,
    @SerializedName("date_of_admission") val dateOfAdmission: String?,

    // Datos de Recepción
    @SerializedName("received_by_tow") val receivedByTow: Boolean? = null,
    @SerializedName("tow_company") val towCompany: String?,
    @SerializedName("tow_driver_name") val towDriverName: String?,

    // Portada
    @SerializedName("cover_photo_url") val coverPhotoUrl: String?,
    @SerializedName("cover_photo_thumb_url") val coverPhotoThumbUrl: String?,

    // Objetos Anidados
    @SerializedName("client") val client: ClientDto?,
    @SerializedName("vehicle") val vehicle: VehicleDto?,
    @SerializedName("insurance_claim") val insuranceClaim: InsuranceClaimDto?
)

data class PhotoDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?
)

data class DamageDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("description") val description: String?,
    @SerializedName("will_be_repaired") val willBeRepaired: Boolean?
)

data class ClientDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("is_government") val isGovernment: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class VehicleDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("color") val color: String?,
    @SerializedName("plates") val plates: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("car_name") val carName: String?,
    @SerializedName("car_brand") val carBrand: String?,
    @SerializedName("car_year") val carYear: Int?,
    @SerializedName("version") val version: VehicleVersionDto?
)

data class VehicleVersionDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?
)

data class InsuranceClaimDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("policy_number") val policyNumber: String?,
    @SerializedName("sinister_number") val sinisterNumber: String?,
    @SerializedName("deductible") val deductible: String?,
    @SerializedName("adjuster_name") val adjusterName: String?,
    @SerializedName("adjuster_phone") val adjusterPhone: String?,
    @SerializedName("claim_date") val claimDate: String?
)