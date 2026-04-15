package com.sunshine.appsuite.budget.settings.system.wallpaper.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.MultiBrowseCarouselStrategy
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.settings.system.wallpaper.WallpaperConstants
import com.sunshine.appsuite.budget.settings.system.wallpaper.WallpaperHelper
import com.sunshine.appsuite.budget.settings.system.wallpaper.WallpaperMode
import com.sunshine.appsuite.budget.databinding.FragmentSettingsWallpaperBinding

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: FragmentSettingsWallpaperBinding
    private lateinit var wallpaperHelper: WallpaperHelper
    private lateinit var wallpaperAdapter: WallpaperCarouselAdapter

    private var isSyncingSwitch = false
    private var isSyncingMode = false
    private var hasHiddenInitialLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentSettingsWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wallpaperHelper = WallpaperHelper(this)
        wallpaperHelper.ensureDefaultSelection()

        setupSystemBars()
        setupToolbar()
        setupCarousel()
        setupLoadWallpapers()
        syncInitialState()
        setupActions()
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

    private fun setupCarousel() {
        wallpaperAdapter = WallpaperCarouselAdapter(
            onWallpaperSelected = { url ->
                // Siempre guarda la selección del carrusel para el modo fijo
                wallpaperHelper.setSelectedWallpaperUrl(url)

                when (wallpaperHelper.getWallpaperMode()) {
                    WallpaperMode.FIXED -> {
                        if (binding.switchWallpaper.isChecked) {
                            applyFixedWallpaper(url)
                        } else {
                            Toast.makeText(
                                this,
                                "Fondo fijo guardado. Se aplicará al activar el switch.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    WallpaperMode.RANDOM -> {
                        Toast.makeText(
                            this,
                            "Modo aleatorio activo. Esta selección quedará guardada para cuando vuelvas a Fijo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onInitialImageResolved = {
                hideWallpaperLoadingOnce()
            }
        )

        val carouselLayoutManager = CarouselLayoutManager(
            MultiBrowseCarouselStrategy()
        ).apply {
            carouselAlignment = CarouselLayoutManager.ALIGNMENT_START
        }

        binding.carouselRecyclerView.apply {
            layoutManager = carouselLayoutManager
            setHasFixedSize(true)
            adapter = wallpaperAdapter
        }

        CarouselSnapHelper().attachToRecyclerView(binding.carouselRecyclerView)

        Log.d("WallpaperActivity", "Multi-browse carousel configurado")
    }

    private fun setupLoadWallpapers() {
        val wallpapers = WallpaperConstants.WALLPAPERS

        showWallpaperLoading()

        Log.d("WallpaperActivity", "Wallpapers: ${wallpapers.size}")

        wallpaperAdapter.resetInitialLoadState()
        wallpaperAdapter.submitList(wallpapers)

        if (wallpapers.isEmpty()) {
            hideWallpaperLoadingOnce()
        }
    }

    private fun setupActions() {
        binding.cardControl.setOnClickListener {
            binding.switchWallpaper.toggle()
        }

        binding.switchWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (isSyncingSwitch) return@setOnCheckedChangeListener

            wallpaperHelper.setWallpaperEnabled(isChecked)

            if (isChecked) {
                Toast.makeText(
                    this,
                    "Activando fondo de AppSuite...",
                    Toast.LENGTH_SHORT
                ).show()

                wallpaperHelper.applyByMode { success ->
                    if (success) {
                        val modeLabel = when (wallpaperHelper.getWallpaperMode()) {
                            WallpaperMode.FIXED -> "fijo"
                            WallpaperMode.RANDOM -> "aleatorio"
                        }

                        Toast.makeText(
                            this,
                            "Fondo de AppSuite activado ($modeLabel).",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        wallpaperHelper.setWallpaperEnabled(false)
                        setSwitchCheckedSilently(false)

                        Toast.makeText(
                            this,
                            "No se pudo activar el fondo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Restaurando fondo por defecto...",
                    Toast.LENGTH_SHORT
                ).show()

                wallpaperHelper.clearAppSuiteWallpaper { success ->
                    if (success) {
                        Toast.makeText(
                            this,
                            "Se restauró el fondo predeterminado de Android.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        wallpaperHelper.setWallpaperEnabled(true)
                        setSwitchCheckedSilently(true)

                        Toast.makeText(
                            this,
                            "No se pudo restaurar el fondo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.toggleWallpaperMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isSyncingMode || !isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                binding.btnModeFixed.id -> {
                    wallpaperHelper.setWallpaperMode(WallpaperMode.FIXED)

                    Toast.makeText(
                        this,
                        "Modo fijo activado.",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (binding.switchWallpaper.isChecked) {
                        val fixedUrl = wallpaperHelper.getSelectedWallpaperUrl()
                            .ifBlank { WallpaperConstants.WALLPAPERS.firstOrNull().orEmpty() }

                        if (fixedUrl.isNotBlank()) {
                            applyFixedWallpaper(fixedUrl)
                        }
                    }
                }

                binding.btnModeRandom.id -> {
                    wallpaperHelper.setWallpaperMode(WallpaperMode.RANDOM)

                    Toast.makeText(
                        this,
                        "Modo aleatorio activado.",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (binding.switchWallpaper.isChecked) {
                        Toast.makeText(
                            this,
                            "Aplicando un fondo aleatorio...",
                            Toast.LENGTH_SHORT
                        ).show()

                        wallpaperHelper.applyByMode { success ->
                            if (!success) {
                                Toast.makeText(
                                    this,
                                    "No se pudo aplicar el fondo aleatorio.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun syncInitialState() {
        setSwitchCheckedSilently(wallpaperHelper.isWallpaperEnabled())
        syncModeSelection()

        if (wallpaperHelper.getSelectedWallpaperUrl().isBlank()) {
            WallpaperConstants.WALLPAPERS.firstOrNull()?.let { firstUrl ->
                wallpaperHelper.setSelectedWallpaperUrl(firstUrl)
            }
        }
    }

    private fun syncModeSelection() {
        isSyncingMode = true

        when (wallpaperHelper.getWallpaperMode()) {
            WallpaperMode.FIXED -> binding.toggleWallpaperMode.check(binding.btnModeFixed.id)
            WallpaperMode.RANDOM -> binding.toggleWallpaperMode.check(binding.btnModeRandom.id)
        }

        isSyncingMode = false
    }

    private fun setSwitchCheckedSilently(checked: Boolean) {
        isSyncingSwitch = true
        binding.switchWallpaper.isChecked = checked
        isSyncingSwitch = false
    }

    private fun showWallpaperLoading() {
        hasHiddenInitialLoading = false
        binding.loadingWallpapers.visibility = View.VISIBLE
    }

    private fun hideWallpaperLoadingOnce() {
        if (hasHiddenInitialLoading) return
        hasHiddenInitialLoading = true
        binding.loadingWallpapers.visibility = View.GONE
    }

    private fun applyFixedWallpaper(url: String) {
        Toast.makeText(this, "Aplicando fondo fijo...", Toast.LENGTH_SHORT).show()

        wallpaperHelper.fetchAndApplyWallpaper(
            url = url,
            saveAsSelected = true
        ) { success ->
            if (success) {
                Log.d("WallpaperActivity", "applyFixedWallpaper url=$url")
            } else {
                Toast.makeText(
                    this,
                    "No se pudo aplicar el fondo fijo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}