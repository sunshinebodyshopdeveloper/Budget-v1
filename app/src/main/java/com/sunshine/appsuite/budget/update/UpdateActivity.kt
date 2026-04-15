package com.sunshine.appsuite.budget.update

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.databinding.ActivityUpdateBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupContent()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupContent() {
        val currentVersionName = getAppVersionName()
        val currentVersionCode = getAppVersionCode()

        binding.tvAppName.text = getString(R.string.app_name)
        binding.tvVersion.text = getString(R.string.about_version_text, currentVersionName)

        binding.cardCoreUpdate.setOnClickListener {
            // 1. Mostramos que estamos trabajando
            showSimpleSnackbar("Buscando actualizaciones...")

            lifecycleScope.launch {
                try {
                    // 2. Configuración rápida de Retrofit (Lo ideal es tenerlo en una clase aparte, pero aquí para rápido)
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://pilotosadac.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val service = retrofit.create(UpdateService::class.java)

                    // 3. Petición al servidor
                    val response = service.getUpdateInfo()

                    // 4. Comparación REAL
                    val currentCode = getAppVersionCode()
                    val currentName = getAppVersionName()

                    checkVersionAndPrompt(
                        currentCode,
                        currentName,
                        response.versionCode,
                        response.versionName,
                        response.apkUrl
                    )

                } catch (e: Exception) {
                    // Si el servidor está caído o no hay internet
                    showSimpleSnackbar("Error al conectar con el servidor")
                }
            }
        }

        binding.cardModulesUpdate.setOnClickListener {
            showSimpleSnackbar("Módulos actualizados")
        }
    }

    private fun checkVersionAndPrompt(
        currentCode: Long,
        currentName: String,
        remoteCode: Long,
        remoteName: String,
        url: String
    ) {
        if (isDownloading) {
            showSimpleSnackbar("Ya hay una descarga en curso...")
            return
        }

        // Comparación lógica e infalible usando el Code
        if (remoteCode > currentCode) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Nueva versión disponible")
                .setMessage("Deseas actualizar a la versión $remoteName? (Actual: $currentName)")
                .setPositiveButton("Descargar") { _, _ ->
                    isDownloading = true
                    startUpdateProcess(url)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        } else {
            showSimpleSnackbar(getString(R.string.update_core_message_latest_version, currentName))
        }
    }

    private fun startUpdateProcess(url: String) {
        // Usamos applicationContext para que el BroadcastReceiver
        // no muera si la Activity se cierra.
        UpdateManager.downloadAndInstall(applicationContext, url)
        showSimpleSnackbar("Descargando actualización...")
    }

    private fun showSimpleSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.cardCoreUpdate)
            .show()
    }

    // --- MÉTODOS DE EXTRACCIÓN DE VERSIÓN ---

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "N/A"
        } catch (e: Exception) { "N/A" }
    }

    private fun getAppVersionCode(): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) { 0L }
    }
}