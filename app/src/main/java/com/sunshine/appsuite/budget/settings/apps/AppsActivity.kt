package com.sunshine.appsuite.budget.settings.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.FragmentSettingsAppsBinding
import java.io.File
import kotlin.collections.forEach

class AppsActivity : AppCompatActivity(), AppsAdapter.ModuleActionListener {

    private lateinit var binding: FragmentSettingsAppsBinding
    private lateinit var appsAdapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSettingsAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupRecycler()
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

    private fun setupToolbar() = with(binding.toolbar) {
        title = getString(R.string.settings_section_apps_title)
        setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecycler() {
        val modules = buildModules()
        // Actualizar estado instalado
        modules.forEach { module ->
            module.packageName?.let { pkg ->
                val installed = isPackageInstalled(pkg)
                module.isInstalled = installed

                // Ahora sí podrás reasignar porque sizeText será 'var'
                if (installed) {
                    module.sizeText = getAppSize(pkg)
                } else {
                    module.sizeText = "---"
                }
            }
        }

        appsAdapter = AppsAdapter(modules, this)
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = appsAdapter
    }

    private fun getAppSize(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val file = File(info.sourceDir)
            val sizeInBytes = file.length()
            val sizeInMb = sizeInBytes / (1024.0 * 1024.0)
            String.format("%.2f MB", sizeInMb)
        } catch (e: Exception) {
            "0.00 MB"
        }
    }

    private fun buildModules(): MutableList<AppModule> {
        val defaultSize = getString(R.string.apps_detail_size_sample)

        return mutableListOf(
            AppModule(
                id = "appsuite",
                nameRes = R.string.apps_module_appsuite_title,
                descriptionRes = R.string.apps_module_appsuite_desc,
                iconRes = R.drawable.ic_app_appsuite,
                packageName = packageName,
                installUrl = null,
                isInstalled = true,
                isExpanded = true,
                sizeText = defaultSize
            ),
            AppModule(
                id = "towing",
                nameRes = R.string.apps_module_towing_title,
                descriptionRes = R.string.apps_module_towing_desc,
                iconRes = R.drawable.ic_app_towing,
                packageName = PKG_TOWING,
                installUrl = URL_TOWING,
                sizeText = defaultSize
            ),
            AppModule(
                id = "tracking",
                nameRes = R.string.apps_module_tracking_title,
                descriptionRes = R.string.apps_module_tracking_desc,
                iconRes = R.drawable.ic_app_tracking,
                packageName = PKG_TRACKING,
                installUrl = URL_TRACKING,
                sizeText = defaultSize
            ),
            AppModule(
                id = "parts",
                nameRes = R.string.apps_module_parts_title,
                descriptionRes = R.string.apps_module_parts_desc,
                iconRes = R.drawable.ic_app_parts,
                packageName = PKG_PARTS,
                installUrl = URL_PARTS,
                sizeText = defaultSize
            ),
            AppModule(
                id = "budget",
                nameRes = R.string.apps_module_budget_title,
                descriptionRes = R.string.apps_module_budget_desc,
                iconRes = R.drawable.ic_app_budget,
                packageName = PKG_BUDGET,
                installUrl = URL_BUDGET,
                sizeText = defaultSize
            ),
            AppModule(
                id = "assignments",
                nameRes = R.string.apps_module_assignments_title,
                descriptionRes = R.string.apps_module_assignments_desc,
                iconRes = R.drawable.ic_app_assignments,
                packageName = PKG_ASSIGNMENTS,
                installUrl = URL_ASSIGNMENTS,
                sizeText = defaultSize
            )
        )
    }

    override fun onPrimaryActionClick(module: AppModule) {
        if (module.isInstalled) {
            module.packageName?.let { pkg ->
                openModule(pkg)
            } ?: Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
        } else {
            module.installUrl?.let { url ->
                openInstallUrl(url)
            } ?: Toast.makeText(this, "Instalación no disponible para este módulo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openModule(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInstallUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    companion object {
        private const val PKG_TOWING = "com.sunshine.appsuite.towing"
        private const val PKG_TRACKING = "com.sunshine.appsuite.tracking"
        private const val PKG_PARTS = "com.sunshine.appsuite.parts"
        private const val PKG_BUDGET = "com.sunshine.appsuite.budget"
        private const val PKG_ASSIGNMENTS = "com.sunshine.appsuite.assignments"
        private const val URL_TOWING = "https://apps.sunshineappsuite.com/towing"
        private const val URL_TRACKING = "https://apps.sunshineappsuite.com/tracking"
        private const val URL_PARTS = "https://apps.sunshineappsuite.com/parts"
        private const val URL_BUDGET = "https://apps.sunshineappsuite.com/budget"
        private const val URL_ASSIGNMENTS = "https://apps.sunshineappsuite.com/assignments"
    }
}