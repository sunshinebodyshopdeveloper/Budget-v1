package com.sunshine.appsuite.budget.data.network

import com.sunshine.appsuite.budget.BuildConfig
import com.sunshine.appsuite.budget.security.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TIMEOUT_SECONDS = 30L

    /**
     * OkHttp con interceptor de Auth + logging. Útil para:
     * - Retrofit
     * - Cargas de imágenes/archivos que también requieren token
     */
    fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            // 👇 Primero scalars para respuestas tipo String (como /api/token)
            .addConverterFactory(ScalarsConverterFactory.create())
            // 👇 Luego Gson para todo lo que sí sea JSON (/api/v1/...)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Compat: quien aún mande TokenManager, recibe un Retrofit listo.
     */
    fun createRetrofit(tokenManager: TokenManager): Retrofit {
        return createRetrofit(createOkHttpClient(tokenManager))
    }
}
