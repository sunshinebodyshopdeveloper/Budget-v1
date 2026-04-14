package com.sunshine.appsuite.budget.orders.repository

import com.sunshine.appsuite.budget.orders.data.ServiceOrderListItemDto
import com.sunshine.appsuite.budget.orders.data.ServiceOrdersApi

class ServiceOrderRepository(
    private val api: ServiceOrdersApi
) {
    suspend fun fetchPage(page: Int, perPage: Int): List<ServiceOrderListItemDto> {
        return api.getServiceOrders(page = page, perPage = perPage).toList()
    }
}
