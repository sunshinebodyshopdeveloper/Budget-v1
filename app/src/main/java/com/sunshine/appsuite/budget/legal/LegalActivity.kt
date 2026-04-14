package com.sunshine.appsuite.budget.legal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sunshine.appsuite.BuildConfig
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityLegalBinding

class LegalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLegalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupContent()
        setupLinks()
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, R.color.google_background_settings)
        val nav = ContextCompat.getColor(this, R.color.google_white)

        window.statusBarColor = status
        window.navigationBarColor = nav

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings_section_legal_title)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupContent() {
        val versionName = BuildConfig.VERSION_NAME
        binding.tvFooter.text = getString(R.string.legal_footer, getString(R.string.app_name), versionName)
    }

    private fun setupLinks() {
        val termsUrl = getString(R.string.legal_terms_url)

        binding.tvLinkDownloadPdf.setOnClickListener { openUrl(termsUrl) }
        binding.tvFullVersionHint.setOnClickListener { openUrl(termsUrl) }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
