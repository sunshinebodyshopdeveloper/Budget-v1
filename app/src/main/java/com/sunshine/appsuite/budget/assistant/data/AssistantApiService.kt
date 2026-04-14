package com.sunshine.appsuite.budget.assistant.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AssistantApiService {
    @GET("api/v1/service-orders/{service_order}")
    suspend fun getServiceOrderDetail(
        @Path("service_order") orderCode: String
    ): ServiceOrderEnvelope

    // CAMBIO: Usamos el endpoint que probaste en Postman
    @GET("api/v1/service-orders")
    suspend fun searchServiceOrders(
        @Query("search") query: String
    ): ServiceOrderListResponse
}