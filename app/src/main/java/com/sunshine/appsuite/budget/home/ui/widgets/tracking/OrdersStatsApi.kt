package com.sunshine.appsuite.budget.home.ui.widgets.tracking

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface OrdersStatsApi {

    // Si tu baseUrl NO incluye /api/v1, deja esto tal cual:
    @GET("api/v1/service-orders/stats")
    suspend fun getOrdersStats(): OrdersStatsResponse

    // Si tu baseUrl YA incluye /api/v1, usa esto y comenta el de arriba:
    // @GET("orders/stats")
    // suspend fun getOrdersStats(): OrdersStatsResponse
}

data class OrdersStatsResponse(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("vehicle_locations") val locations: VehicleLocations = VehicleLocations()
) {
    // Helpers para no romper tu lógica en el Controller
    val onFloor: Int get() = locations.floor
    val inTransit: Int get() = locations.transit
    val pendingEntry: Int get() = locations.pendingEntry
}

data class VehicleLocations(
    @SerializedName("floor") val floor: Int = 0,
    @SerializedName("transit") val transit: Int = 0,
    @SerializedName("pending_entry") val pendingEntry: Int = 0
)
