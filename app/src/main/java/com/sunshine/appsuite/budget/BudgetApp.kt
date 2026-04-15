package com.sunshine.appsuite.budget

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.data.network.AuthApi
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.budget.settings.appearance.ThemePreferences
import com.sunshine.appsuite.budget.settings.system.SystemPreferences
import com.sunshine.appsuite.budget.settings.system.wallpaper.WallpaperHelper
import com.sunshine.appsuite.budget.shortcuts.AppShortcutsToggle
import com.sunshine.appsuite.budget.shortcuts.ShortcutsNotifier
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

    override fun onCreate() {
        super.onCreate()

        initializeWallpaperControl()

        ThemePreferences.apply(this)
        DynamicColors.applyToActivitiesIfAvailable(this)

        ShortcutsNotifier.ensureChannel(this)

        runCatching {
            val enabled = SystemPreferences.isShortcutsEnabled(this)
            AppShortcutsToggle.setEnabled(this, enabled)
        }

        tokenManager = TokenManager(this)

        okHttpClient = ApiClient.createOkHttpClient(tokenManager)
        retrofit = ApiClient.createRetrofit(okHttpClient)

        authApi = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        ordersApi = retrofit.create(OrdersApi::class.java)

        userProfileStore = UserProfileStore(
            appContext = this,
            tokenManager = tokenManager,
            userApi = userApi
        )

        userProfileStore.ensureFresh()
    }

    private fun initializeWallpaperControl() {
        val helper = WallpaperHelper(this)

        // Opción recomendada:
        // No reaplicar nada automáticamente en cada arranque.
        // Solo aseguramos que exista una selección inicial por defecto.
        helper.ensureDefaultSelection()

        // Si de verdad quieres que al iniciar se respete el estado guardado
        // y se reaplique SOLO si está activo, usa esto:
        // helper.syncWallpaperOnAppStart()
    }
}
