package com.sunshine.appsuite.budget.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sunshine.appsuite.budget.legal.AboutActivity
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.budget.legal.LegalActivity
import com.sunshine.appsuite.budget.LoginActivity
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.menu.ProfileMenuListener
import com.sunshine.appsuite.budget.settings.account.SettingsAccountFragment
import com.sunshine.appsuite.budget.settings.permissions.SettingsPermissionsFragment
import com.sunshine.appsuite.databinding.ActivitySettingsBinding
import com.sunshine.appsuite.budget.settings.security.SettingsSecurityFragment
import com.sunshine.appsuite.budget.settings.support.SettingsSupportFragment
import com.sunshine.appsuite.budget.settings.system.SettingsSystemFragment

class SettingsActivity : AppCompatActivity(),
    SettingsNavigator,
    ProfileMenuListener,
    SettingsHomeFragment.SettingsHomeHost,
    SettingsAccountFragment.SettingsAccountHost,
    SettingsSecurityFragment.SettingsSecurityHost,
    SettingsPermissionsFragment.SettingsPermissionsHost{

    enum class Section {
        HOME,
        ACCOUNT,
        SECURITY,
        PERMISSIONS,
        SUPPORT,
        SYSTEM
    }

    companion object {
        private const val EXTRA_SECTION = "extra_section"

        fun start(context: Context, section: Section = Section.HOME) {
            val intent = Intent(context, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SECTION, section.name)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()

        supportFragmentManager.addOnBackStackChangedListener {
            updateTitleForCurrentFragment()
            updateNavigationIcon()
        }

        if (savedInstanceState == null) {
            val sectionName = intent.getStringExtra(EXTRA_SECTION)
            val section = sectionName
                ?.let { runCatching { Section.valueOf(it) }.getOrNull() }
                ?: Section.HOME

            openSection(section, addToBackStack = false)
        } else {
            updateTitleForCurrentFragment()
            updateNavigationIcon()
        }
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, R.color.google_background_settings)
        val nav = ContextCompat.getColor(this, R.color.google_background_settings)

        window.statusBarColor = status
        window.navigationBarColor = nav

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
        updateNavigationIcon()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_help -> {
                openSection(Section.SUPPORT, addToBackStack = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTitleForCurrentFragment() {
        val current = supportFragmentManager.findFragmentById(R.id.settingsContainer)

        val titleRes = when (current) {
            is SettingsHomeFragment -> R.string.settings_title
            is SettingsAccountFragment -> R.string.settings_section_account_title
            is SettingsSecurityFragment -> R.string.settings_section_security_title
            is SettingsPermissionsFragment -> R.string.settings_section_permissions_title
            is SettingsSupportFragment -> R.string.settings_section_support_title
            is SettingsSystemFragment -> R.string.settings_section_system_title
            else -> R.string.settings_title
        }

        supportActionBar?.title = getString(titleRes)
    }

    private fun updateNavigationIcon() {
        val hasBackStack = supportFragmentManager.backStackEntryCount > 0

        val iconRes = if (hasBackStack) {
            R.drawable.ic_arrow_back
        } else {
            R.drawable.ic_close
        }

        supportActionBar?.setHomeAsUpIndicator(iconRes)
    }

    override fun openSection(section: Section, addToBackStack: Boolean) {
        val fragment = when (section) {
            Section.HOME -> SettingsHomeFragment()
            Section.ACCOUNT -> SettingsAccountFragment()
            Section.SECURITY -> SettingsSecurityFragment()
            Section.PERMISSIONS -> SettingsPermissionsFragment()
            Section.SUPPORT -> SettingsSupportFragment()
            Section.SYSTEM -> SettingsSystemFragment()
        }

        val tx = supportFragmentManager.beginTransaction()
            .replace(R.id.settingsContainer, fragment)

        if (addToBackStack) {
            tx.addToBackStack(section.name)
        }

        tx.commit()

        updateTitleForCurrentFragment()
        updateNavigationIcon()
    }

    private fun performLogout() {
        val app = application as AppSuiteApp
        app.tokenManager.clearToken()

        app.userProfileStore.clear()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onProfileLogout() {
        performLogout()
    }

    override fun onProfileOpenAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onProfileOpenLegal() {
        startActivity(Intent(this, LegalActivity::class.java))
    }

    override fun onOpenSettingsAdmin() {
        startActivity(Intent(this, LegalActivity::class.java))
    }

    override fun onOpenSupport() {
        openSection(Section.SUPPORT, addToBackStack = true)
    }

    override fun onOpenSystem() {
        openSection(Section.SYSTEM, addToBackStack = true)
    }

    override fun onOpenAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onOpenLegal() {
        startActivity(Intent(this, LegalActivity::class.java))
    }

    override fun onRunSecurityCheck() {
        Toast.makeText(this, "Chequeo de seguridad ejecutado.", Toast.LENGTH_SHORT).show()
    }
}
