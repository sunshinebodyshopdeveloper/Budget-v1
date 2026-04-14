package com.sunshine.appsuite.budget.home.ui.widgets.tracking

class OrdersStatsRepository(
    private val api: OrdersStatsApi
) {
    suspend fun fetch(): OrdersStatsResponse = api.getOrdersStats()
}
