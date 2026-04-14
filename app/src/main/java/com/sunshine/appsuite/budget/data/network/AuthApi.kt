package com.sunshine.appsuite.budget.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/token")
    suspend fun login(
        @Body request: LoginRequest
    ): String
}

// Request al endpoint /api/token
data class LoginRequest(
    val username: String,
    val password: String,
    val device_name: String
)
