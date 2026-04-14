package com.sunshine.appsuite.budget.settings.system

import android.content.Context

object SystemPreferences {

    private const val PREFS_NAME = "system_prefs"
    private const val KEY_SHORTCUTS_ENABLED = "shortcuts_enabled"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    fun isShortcutsEnabled(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHORTCUTS_ENABLED, true)
    }

    fun setShortcutsEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHORTCUTS_ENABLED, enabled)
            .apply()
    }

    fun isNotificationsEnabled(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    fun setNotificationsEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }
}
