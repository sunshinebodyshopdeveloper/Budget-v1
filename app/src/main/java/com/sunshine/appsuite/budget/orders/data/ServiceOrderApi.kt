package com.sunshine.appsuite.budget.orders.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Paginación: page + per_page
 * Orden: por default intentamos sort=-id (si el backend lo ignora, no pasa nada).
 */
interface ServiceOrdersApi {

    @GET("api/v1/service-orders")
    suspend fun getServiceOrders(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "-id"
    ): ServiceOrdersResponse
}

data class ServiceOrdersResponse(
    @SerializedName("data") val data: JsonElement?,
    @SerializedName("meta") val meta: PaginationMeta? = null
) {
    fun toList(): List<ServiceOrderListItemDto> {
        if (data == null || data.isJsonNull) return emptyList()
        return try {
            when {
                data.isJsonArray -> data.asJsonArray.mapNotNull { el ->
                    el.asJsonObject?.let { obj -> ServiceOrderListItemDto.from(obj) }
                }
                data.isJsonObject -> listOfNotNull(ServiceOrderListItemDto.from(data.asJsonObject))
                else -> emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)
