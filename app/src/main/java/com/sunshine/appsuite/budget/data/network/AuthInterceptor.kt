package com.sunshine.appsuite.budget.data.network

import com.sunshine.appsuite.budget.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        val newRequestBuilder = originalRequest.newBuilder()

        // ✅ Esto hace que Laravel te responda JSON (incluyendo errores), no HTML
        newRequestBuilder.header("Accept", "application/json")
        newRequestBuilder.header("X-Requested-With", "XMLHttpRequest")

        if (!token.isNullOrEmpty()) {
            newRequestBuilder.header("Authorization", "Bearer $token")
        }

        val newRequest = newRequestBuilder.build()
        return chain.proceed(newRequest)
    }
}
