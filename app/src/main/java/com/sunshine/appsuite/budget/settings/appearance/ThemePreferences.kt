package com.sunshine.appsuite.budget.settings.appearance

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun getNightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context))
    }

    fun setNightMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isDarkActive(context: Context): Boolean {
        return when (getNightMode(context)) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun toggle(context: Context) {
        val newMode = if (isDarkActive(context)) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        setNightMode(context, newMode)
    }
}
