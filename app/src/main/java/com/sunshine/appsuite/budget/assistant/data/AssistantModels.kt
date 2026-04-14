package com.sunshine.appsuite.budget.assistant.data

import com.google.gson.annotations.SerializedName

data class ServiceOrderListResponse(
    val data: List<ServiceOrderResponse>
)

data class SearchIndex(
    val indexUid: String,
    val hits: List<ServiceOrderResponse>
)

data class ServiceOrderEnvelope(
    val data: ServiceOrderResponse
)

data class ServiceOrderResponse(
    val id: Int?,
    val code: String?,
    @SerializedName("client_name") val clientName: String?,
    val status: String?,
    @SerializedName("created_at") val createdAt: String?,

    // CAMBIO CRÍTICO: Recibir como Any o Int para evitar crash de tipos
    @SerializedName("vehicle_documented") val vehicleDocumented: Any?,

    val client: ClientDetails?,
    val vehicle: VehicleDetails?,
    @SerializedName("insurance_claim") val insuranceClaim: InsuranceClaim?,
    @SerializedName("work_orders") val workOrders: List<WorkOrder>?
)

data class ClientDetails(val name: String?, val phone: String?, val email: String?)

data class VehicleDetails(
    val color: String?,
    val plates: String?,
    @SerializedName("car_name") val carName: String?,
    @SerializedName("car_brand") val carBrand: String?
)

data class InsuranceClaim(
    @SerializedName("policy_number") val policyNumber: String?,
    @SerializedName("sinister_number") val sinisterNumber: String?,
    val deductible: String?
)

data class WorkOrder(
    val code: String?,
    val status: String?,
    @SerializedName("repair_stage") val repairStage: String?
)