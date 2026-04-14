package com.sunshine.appsuite.budget.orders.repository

import com.sunshine.appsuite.budget.orders.data.OrderDto
import com.sunshine.appsuite.budget.orders.data.OrdersApi

class OrdersRepository(
    private val api: OrdersApi
) {
    suspend fun fetch(orderCode: String): OrderDto {
        return api.getOrder(orderCode).data
    }
}