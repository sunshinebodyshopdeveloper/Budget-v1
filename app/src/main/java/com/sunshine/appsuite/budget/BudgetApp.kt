package com.sunshine.appsuite.budget

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.data.network.AuthApi
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.budget.settings.appearance.ThemePreferences
import com.sunshine.appsuite.budget.settings.system.wallpaper.WallpaperHelper
import com.sunshine.appsuite.budget.user.UserProfileStore
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

    lateinit var userProfileStore: UserProfileStore
        private set

    /**
     * Intenta recuperar el token desde AppSuite.
     * NOTA: Para que esto funcione al 100% con EncryptedSharedPreferences,
     * el TokenManager debe ser capaz de inicializarse con el contexto de la otra app.
     */
    private fun tryInheritSession() {
        if (tokenManager.hasToken()) return

        try {
            // Intentamos usar la función que pusimos en el nuevo TokenManager
            val sharedToken = tokenManager.getInheritedToken()

            android.util.Log.d("BudgetDebug", "Token heredado: $sharedToken")

            if (!sharedToken.isNullOrBlank()) {
                tokenManager.saveToken(sharedToken)
            }
        } catch (e: Exception) {
            // Si falla (ej. AppSuite no instalada), no pasa nada, pedirá login normal
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Configuraciones visuales básicas
        initializeWallpaperControl()
        ThemePreferences.apply(this)
        DynamicColors.applyToActivitiesIfAvailable(this)

        // 2. Inicializar Manager de seguridad
        tokenManager = TokenManager(this)

        // 3. Intentar "robar" la sesión de la app madre
        tryInheritSession()

        // 4. Configurar red con el token (heredado o propio)
        okHttpClient = ApiClient.createOkHttpClient(tokenManager)
        retrofit = ApiClient.createRetrofit(okHttpClient)

        // 5. Inicializar APIs
        authApi = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        ordersApi = retrofit.create(OrdersApi::class.java)

        // 6. Fuente única de verdad para el perfil
        userProfileStore = UserProfileStore(
            appContext = this,
            tokenManager = tokenManager,
            userApi = userApi
        )

        // Si logramos heredar el token, esto descargará los datos del usuario automáticamente
        userProfileStore.ensureFresh()
    }

    private fun initializeWallpaperControl() {
        val helper = WallpaperHelper(this)
        helper.ensureDefaultSelection()
    }
}