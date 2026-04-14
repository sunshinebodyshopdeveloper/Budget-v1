package com.sunshine.appsuite.budget.settings.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityPasswordBinding

class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureSystemBars()
        setupToolbar()
        // Aquí luego conectamos la lógica real de cambio de contraseña
    }

    private fun configureSystemBars() {
        val bgColor = ContextCompat.getColor(this, R.color.google_background_settings)

        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.settings_section_password_title)
            setDisplayHomeAsUpEnabled(true)
        }

        // Flecha back
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}