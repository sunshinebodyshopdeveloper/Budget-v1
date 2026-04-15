package com.sunshine.appsuite.budget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.sunshine.appsuite.budget.databinding.ActivityMainBinding
import com.sunshine.appsuite.budget.settings.security.SecurityAdvisoryNotifier
import com.sunshine.appsuite.budget.system.network.NetworkUtils
import com.sunshine.appsuite.budget.system.network.NoInternetActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sunshine.appsuite.budget.user.ui.ProfileBottomSheetDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(){

    private var backToExit = false
    private var backToExitJob: Job? = null
    private var isMainUiShown = false

    // Binding para la UI REAL (home con drawer + bottom nav)
    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as BudgetApp }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Si no hay internet: nos vamos y ya (sin montar UI)
        if (!NetworkUtils.isOnline(this)) {
            startActivity(Intent(this, NoInternetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        // Arranque sin UI custom: resolvemos lógica y luego mostramos UI real
        lifecycleScope.launch {

            // Token
            if (!app.tokenManager.hasToken()) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Ya puede ver el home → montamos la UI real
            showMainUi(savedInstanceState)
        }

        setupDoubleBackToExit()
    }

    override fun onResume() {
        super.onResume()
        if (isMainUiShown) {
            SecurityAdvisoryNotifier.maybeShow(this)
        }
    }

    // ------------------------
    //   UI REAL (home / tabs)
    // ------------------------

    private fun showMainUi(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        isMainUiShown = true
        SecurityAdvisoryNotifier.maybeShow(this)
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, R.color.google_white)
        val nav = ContextCompat.getColor(this, R.color.google_white)

        window.statusBarColor = status
        window.navigationBarColor = nav

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    private fun setupDoubleBackToExit() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                    return
                }

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
