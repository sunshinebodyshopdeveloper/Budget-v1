package com.sunshine.appsuite.budget

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.sunshine.appsuite.budget.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupContent()
    }

    private fun setupSystemBars() {
        // Usa el colorSurface del theme (cambia solo si algún día activas night bien)
        val surfaceColor = MaterialColors.getColor(
            this,
            R.attr.colorSurface,
            ContextCompat.getColor(this, com.sunshine.appsuite.budget.R.color.google_white)
        )

        window.statusBarColor = surfaceColor
        window.navigationBarColor = surfaceColor

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = !isDarkMode
        controller.isAppearanceLightNavigationBars = !isDarkMode
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(com.sunshine.appsuite.budget.R.string.about_title)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupContent() {
        binding.tvAppName.text = getString(com.sunshine.appsuite.budget.R.string.app_name)

        val versionName = BuildConfig.VERSION_NAME
        binding.tvVersion.text = getString(com.sunshine.appsuite.budget.R.string.about_version_text, versionName)

        binding.tvFooter.text = getString(com.sunshine.appsuite.budget.R.string.about_footer)
    }
}
