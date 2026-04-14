package com.sunshine.appsuite.budget.shortcuts

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sunshine.appsuite.budget.MainActivity
import com.sunshine.appsuite.budget.settings.system.SystemPreferences

class ShortcutRouterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SystemPreferences.isShortcutsEnabled(this)) {
            ShortcutsNotifier.notifyShortcutsDisabledAttempt(this)

            // Intentamos llevarlo a tu SettingsSystemFragment vía MainActivity (si lo soportas)
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(ShortcutsNotifier.EXTRA_DEEPLINK_DEST, ShortcutsNotifier.DEST_SETTINGS_SYSTEM)
                    putExtra(ShortcutsNotifier.EXTRA_SETTINGS_FOCUS, ShortcutsNotifier.FOCUS_NOTIFICATIONS)
                }
            )
            finish()
            return
        }

        val key = AppShortcuts.extractKey(intent?.data)
        AppShortcuts.open(this, key)
        finish()
    }
}
