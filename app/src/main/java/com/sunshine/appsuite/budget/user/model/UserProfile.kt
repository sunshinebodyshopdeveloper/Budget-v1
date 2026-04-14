package com.sunshine.appsuite.budget.user.model

/**
 * Modelo de dominio: lo que la app necesita para mostrar el perfil.
 * (No es el JSON crudo del backend)
 */
data class UserProfile(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null
) {
    fun displayName(fallback: String): String = name?.takeIf { it.isNotBlank() } ?: fallback
    fun displayEmail(fallback: String): String = email?.takeIf { it.isNotBlank() } ?: fallback
}
