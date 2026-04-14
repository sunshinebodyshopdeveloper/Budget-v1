package com.sunshine.appsuite.budget.settings.system

import android.content.Context

object SystemSettingsPreferences {

    private const val PREFS_NAME = "app_prefs" // mismo que ThemePreferences :contentReference[oaicite:8]{index=8}
    private const val KEY_SHORTCUTS = "system_shortcuts_enabled"
    private const val KEY_NOTIFICATIONS = "system_notifications_enabled"

    fun isShortcutsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHORTCUTS, true)
    }

    fun setShortcutsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHORTCUTS, enabled)
            .apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
    }
}
