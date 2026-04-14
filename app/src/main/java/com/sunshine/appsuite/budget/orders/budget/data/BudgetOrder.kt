package com.sunshine.appsuite.budget.orders.budget.data

data class BudgetOrder(
    val id: Int,
    val folio: String,
    val clientName: String,
    val vehiclePlate: String,
    val vehicleModel: String,
    val status: String // "assigned"
)