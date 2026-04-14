package com.sunshine.appsuite.budget.settings.security

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.settings.SettingsActivity

object SecurityAdvisoryNotifier {

    const val NOTIFICATION_ID = 911

    private const val CHANNEL_ID = "security_advice_v1"
    private const val CHANNEL_NAME = "Seguridad"
    private const val CHANNEL_DESC = "Recomendaciones de seguridad para proteger tu cuenta."

    const val ACTION_SNOOZE_7_DAYS = "com.sunshine.appsuite.action.SECURITY_ADVICE_SNOOZE_7D"
    const val ACTION_OPT_OUT = "com.sunshine.appsuite.action.SECURITY_ADVICE_OPT_OUT"

    const val SNOOZE_7_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
    private const val MIN_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L

    /**
     * Llamar desde MainActivity (cuando ya hay sesión).
     * - Si puede notificar -> notificación con acciones.
     * - Si no puede (permiso/notifs apagadas) -> diálogo in-app (misma lógica + acciones).
     */
    fun maybeShow(activityContext: Context) {
        val ctx = activityContext.applicationContext

        // 1) Si ya tiene App Lock (PIN o Huella), no hay nada que avisar
        val hasAppLock = SecurityPreferences.isPinEnabled(ctx) || SecurityPreferences.isBiometricEnabled(ctx)
        if (hasAppLock) return

        // 2) Respeta "bajo mi responsabilidad"
        if (SecurityAdvisoryPreferences.isOptedOut(ctx)) return

        val now = System.currentTimeMillis()

        // 3) Respeta snooze
        val snoozeUntil = SecurityAdvisoryPreferences.getSnoozeUntil(ctx)
        if (now < snoozeUntil) return

        // 4) Rate limit
        val lastShown = SecurityAdvisoryPreferences.getLastShown(ctx)
        if (lastShown > 0L && (now - lastShown) < MIN_INTERVAL_MS) return

        // Marcamos mostrado ya, para evitar loops si algo truena
        SecurityAdvisoryPreferences.setLastShown(ctx, now)

        ensureChannel(ctx)

        val deviceSecure = isDeviceSecure(ctx)

        val title = ctx.getString(R.string.security_advisory_title)
        val text = if (!deviceSecure) {
            ctx.getString(R.string.security_advisory_text_device_insecure)
        } else {
            ctx.getString(R.string.security_advisory_text_app_insecure)
        }

        // 5) Si puede notificar, notifica. Si no, diálogo in-app.
        if (canPostNotifications(ctx)) {
            postNotification(ctx, title, text)
        } else {
            showFallbackDialog(activityContext, title, text)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        nm.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        val nm = NotificationManagerCompat.from(context)

        // Si el usuario apagó notificaciones a nivel sistema -> respetamos
        if (!nm.areNotificationsEnabled()) return false

        // Android 13+: permiso runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        return true
    }

    private fun postNotification(context: Context, title: String, text: String) {
        val nm = NotificationManagerCompat.from(context)

        val openSecurity = PendingIntent.getActivity(
            context,
            100,
            settingsSecurityIntent(context),
            pendingFlags()
        )

        val snooze = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, SecurityAdvisoryReceiver::class.java).apply {
                action = ACTION_SNOOZE_7_DAYS
            },
            pendingFlags()
        )

        val optOut = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, SecurityAdvisoryReceiver::class.java).apply {
                action = ACTION_OPT_OUT
            },
            pendingFlags()
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_appsuite)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setTimeoutAfter(12000)
            .setContentIntent(openSecurity)
            .addAction(0, context.getString(R.string.security_advisory_action_enable), openSecurity)
            .addAction(0, context.getString(R.string.security_advisory_action_snooze), snooze)
            .addAction(0, context.getString(R.string.security_advisory_action_opt_out), optOut)

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showFallbackDialog(activityContext: Context, title: String, text: String) {
        // Si no tenemos Activity real, no mostramos diálogo
        // (en tu caso lo llamaremos desde MainActivity, así que sí aplica)
        runCatching {
            MaterialAlertDialogBuilder(activityContext)
                .setTitle(title)
                .setMessage(text)
                .setCancelable(true)
                .setPositiveButton(activityContext.getString(R.string.security_advisory_action_enable)) { _, _ ->
                    activityContext.startActivity(settingsSecurityIntent(activityContext))
                }
                .setNeutralButton(activityContext.getString(R.string.security_advisory_action_snooze)) { _, _ ->
                    val now = System.currentTimeMillis()
                    SecurityAdvisoryPreferences.setSnoozeUntil(
                        activityContext.applicationContext,
                        now + SNOOZE_7_DAYS_MS
                    )
                }
                .setNegativeButton(activityContext.getString(R.string.security_advisory_action_opt_out)) { _, _ ->
                    SecurityAdvisoryPreferences.setOptedOut(activityContext.applicationContext, true)
                }
                .show()
        }
    }

    private fun settingsSecurityIntent(context: Context): Intent {
        return Intent(context, SettingsActivity::class.java).apply {
            putExtra("extra_section", SettingsActivity.Section.SECURITY.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun isDeviceSecure(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    private fun pendingFlags(): Int {
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) base or PendingIntent.FLAG_IMMUTABLE else base
    }
}
