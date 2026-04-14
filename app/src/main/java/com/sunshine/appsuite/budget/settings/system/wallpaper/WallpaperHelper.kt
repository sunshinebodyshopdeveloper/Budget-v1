package com.sunshine.appsuite.budget.settings.system.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WallpaperMode {
    FIXED,
    RANDOM
}

class WallpaperHelper(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "appsuite_wallpaper_prefs"
        private const val KEY_ENABLED = "wallpaper_enabled"
        private const val KEY_SELECTED_URL = "selected_wallpaper_url"
        private const val KEY_MODE = "wallpaper_mode"
    }

    private val wallpaperManager = WallpaperManager.getInstance(context)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWallpaperEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).commit()
    }

    fun getSelectedWallpaperUrl(): String {
        return prefs.getString(KEY_SELECTED_URL, "").orEmpty()
    }

    fun setSelectedWallpaperUrl(url: String) {
        prefs.edit().putString(KEY_SELECTED_URL, url).commit()
    }

    fun ensureDefaultSelection() {
        if (getSelectedWallpaperUrl().isBlank()) {
            WallpaperConstants.WALLPAPERS.firstOrNull()?.let { firstUrl ->
                setSelectedWallpaperUrl(firstUrl)
            }
        }
    }

    fun getWallpaperMode(): WallpaperMode {
        val raw = prefs.getString(KEY_MODE, WallpaperMode.FIXED.name)
            ?: WallpaperMode.FIXED.name

        return WallpaperMode.entries.firstOrNull { it.name == raw }
            ?: WallpaperMode.FIXED
    }

    fun setWallpaperMode(mode: WallpaperMode) {
        prefs.edit().putString(KEY_MODE, mode.name).commit()
    }

    fun getRandomWallpaperUrl(): String {
        return WallpaperConstants.WALLPAPERS.random()
    }

    fun getResolvedWallpaperUrl(): String {
        return when (getWallpaperMode()) {
            WallpaperMode.RANDOM -> getRandomWallpaperUrl()
            WallpaperMode.FIXED -> getSelectedWallpaperUrl()
                .ifBlank { WallpaperConstants.WALLPAPERS.firstOrNull().orEmpty() }
        }
    }

    fun applyByMode(onResult: ((Boolean) -> Unit)? = null) {
        val url = getResolvedWallpaperUrl()

        if (url.isBlank()) {
            onResult?.invoke(false)
            return
        }

        val saveAsSelected = getWallpaperMode() == WallpaperMode.FIXED
        fetchAndApplyWallpaper(
            url = url,
            saveAsSelected = saveAsSelected,
            onResult = onResult
        )
    }

    fun fetchAndApplyWallpaper(
        url: String,
        saveAsSelected: Boolean = true,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (saveAsSelected) {
            setSelectedWallpaperUrl(url)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)

                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap
                    val applied = applyWallpaper(bitmap)

                    if (applied) {
                        setWallpaperEnabled(true)
                    }

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(applied)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    fun clearAppSuiteWallpaper(onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.clear(WallpaperManager.FLAG_SYSTEM)
                } else {
                    wallpaperManager.clear()
                }

                setWallpaperEnabled(false)

                withContext(Dispatchers.Main) {
                    onResult?.invoke(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    private suspend fun applyWallpaper(bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(
                        bitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}