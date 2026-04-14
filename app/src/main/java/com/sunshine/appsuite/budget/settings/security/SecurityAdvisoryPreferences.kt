package com.sunshine.appsuite.budget.settings.security

import android.content.Context
import androidx.core.content.edit

object SecurityAdvisoryPreferences {

    private const val PREFS_NAME = "security_advisory_prefs"

    private const val KEY_OPT_OUT = "opt_out"
    private const val KEY_SNOOZE_UNTIL = "snooze_until"
    private const val KEY_LAST_SHOWN = "last_shown"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOptedOut(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OPT_OUT, false)

    fun setOptedOut(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_OPT_OUT, value) }
    }

    fun getSnoozeUntil(context: Context): Long =
        prefs(context).getLong(KEY_SNOOZE_UNTIL, 0L)

    fun setSnoozeUntil(context: Context, epochMillis: Long) {
        prefs(context).edit { putLong(KEY_SNOOZE_UNTIL, epochMillis) }
    }

    fun getLastShown(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SHOWN, 0L)

    fun setLastShown(context: Context, epochMillis: Long) {
        prefs(context).edit { putLong(KEY_LAST_SHOWN, epochMillis) }
    }

    fun clear(context: Context) {
        prefs(context).edit {
            remove(KEY_OPT_OUT)
            remove(KEY_SNOOZE_UNTIL)
            remove(KEY_LAST_SHOWN)
        }
    }
}
