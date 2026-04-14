package com.sunshine.appsuite.budget.settings.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class SecurityAdvisoryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val ctx = context.applicationContext
        val action = intent?.action ?: return

        when (action) {
            SecurityAdvisoryNotifier.ACTION_SNOOZE_7_DAYS -> {
                val now = System.currentTimeMillis()
                SecurityAdvisoryPreferences.setSnoozeUntil(
                    ctx,
                    now + SecurityAdvisoryNotifier.SNOOZE_7_DAYS_MS
                )
                NotificationManagerCompat.from(ctx)
                    .cancel(SecurityAdvisoryNotifier.NOTIFICATION_ID)
            }

            SecurityAdvisoryNotifier.ACTION_OPT_OUT -> {
                SecurityAdvisoryPreferences.setOptedOut(ctx, true)
                NotificationManagerCompat.from(ctx)
                    .cancel(SecurityAdvisoryNotifier.NOTIFICATION_ID)
            }
        }
    }
}
