package com.sunshine.appsuite.budget

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.data.network.AuthApi
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.security.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class BudgetApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var retrofit: Retrofit
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var authApi: AuthApi
        private set

    lateinit var userApi: UserApi
        private set

    lateinit var ordersApi: OrdersApi
        private set

    private fun tryInheritSession() {
        if (tokenManager.hasToken()) return

        try {
            val sharedToken = tokenManager.getInheritedToken()

            android.util.Log.d("BudgetDebug", "Token heredado: $sharedToken")

            if (!sharedToken.isNullOrBlank()) {
                tokenManager.saveToken(sharedToken)
            }
        } catch (e: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        tokenManager = TokenManager(this)

        tryInheritSession()

        okHttpClient = ApiClient.createOkHttpClient(tokenManager)
        retrofit = ApiClient.createRetrofit(okHttpClient)

        authApi = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        ordersApi = retrofit.create(OrdersApi::class.java)
    }
}