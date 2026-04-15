package com.sunshine.appsuite.budget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.sunshine.appsuite.budget.databinding.ActivityMainBinding
import com.sunshine.appsuite.budget.system.network.NetworkUtils
import com.sunshine.appsuite.budget.system.network.NoInternetActivity
import com.sunshine.appsuite.budget.tools.qr.SmartScannerActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var backToExit = false
    private var backToExitJob: Job? = null

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as BudgetApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1. Validación de Red
        if (!NetworkUtils.isOnline(this)) {
            startActivity(Intent(this, NoInternetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        // 2. Validación de Sesión y Carga de UI
        lifecycleScope.launch {
            if (!app.tokenManager.hasToken()) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Si hay token, mostramos el Dashboard
            showMainUi()

            // OPCIONAL: Si quieres que la primera vez que abra la app lance el scanner solo:
            // if (savedInstanceState == null) {
            //    startActivity(Intent(this@MainActivity, SmartScannerActivity::class.java))
            // }
        }

        setupDoubleBackToExit()
    }

    /**
     * Configura el Dashboard Operativo (Estilo XOCARS)
     */
    private fun showMainUi() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSystemBars()

        // El Rail ahora vive aquí y controla el Dashboard
        binding.navigationRail.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_inspection -> {
                    // Acción: Refrescar Dashboard
                    true
                }
                R.id.nav_history -> {
                    // Acción: Ir a historial de presupuestos
                    true
                }
                else -> false
            }
        }

        // El Botón Hero del XML que hicimos para lanzar el scanner
        binding.btnStartScan.setOnClickListener {
            startActivity(Intent(this, SmartScannerActivity::class.java))
        }
    }

    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val color = ContextCompat.getColor(this, R.color.google_white)
        window.statusBarColor = color
        window.navigationBarColor = color

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
    }

    private fun setupDoubleBackToExit() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backToExit) {
                    finishAffinity()
                    return
                }

                backToExit = true
                Toast.makeText(this@MainActivity, getString(R.string.back_exit_confirm), Toast.LENGTH_SHORT).show()

                backToExitJob?.cancel()
                backToExitJob = lifecycleScope.launch {
                    delay(1800)
                    backToExit = false
                }
            }
        })
    }
}