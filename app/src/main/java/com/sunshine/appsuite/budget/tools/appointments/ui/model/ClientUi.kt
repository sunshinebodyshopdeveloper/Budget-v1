package com.sunshine.appsuite.budget.tools.appointments.ui.model

/**
 * Modelo de UI para dropdown:
 * - en pantalla se muestra 'name'
 * - en API/DB se guarda 'id'
 */
data class ClientUi(
    val id: Long,
    val name: String,
    val type: Int
) {
    override fun toString(): String = name
}
