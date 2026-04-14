package com.sunshine.appsuite.budget.shortcuts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sunshine.appsuite.budget.MainActivity
import com.sunshine.appsuite.R

object ShortcutsNotifier {

    /**
     * Android 8+ manda con CANALES.
     * Si ya existía un canal viejo, no puedes cambiar sonido/vibración/badge por código.
     * Por eso el _v2: para aplicar el behavior pro sin reinstalar.
     */
    private const val CHANNEL_ID = "shortcuts_state_v2"
    private const val CHANNEL_NAME = "Accesos rápidos"

    private const val PREFS = "shortcuts_notifier_prefs"
    private const val KEY_LAST_STATE = "last_state" // -1 unknown, 0 off, 1 on

    // Deep-link interno (MainActivity decide cómo navegar)
    const val EXTRA_DEEPLINK_DEST = "extra_destination"
    const val DEST_SETTINGS_SYSTEM = "settings_system"

    const val EXTRA_SETTINGS_FOCUS = "settings_focus"
    const val FOCUS_NOTIFICATIONS = "notifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Avisos de estado de accesos rápidos (shortcuts)."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        nm.createNotificationChannel(channel)
    }

    fun notifyToggleChanged(context: Context, enabled: Boolean) {
        // Evita duplicados
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getInt(KEY_LAST_STATE, -1)
        val now = if (enabled) 1 else 0
        if (last == now) return
        prefs.edit().putInt(KEY_LAST_STATE, now).apply()

        ensureChannel(context)

        val title = if (enabled) "Accesos rápidos activados" else "Accesos rápidos desactivados"
        val text = if (enabled) {
            "Fotos, QR y Soporte ya están disponibles desde el launcher."
        } else {
            "Puedes reactivarlos desde Ajustes."
        }

        postOrFallbackToast(
            context = context,
            notificationId = 701,
            title = title,
            text = text,
            addOpenSettingsAction = true
        )
    }

    fun notifyShortcutsDisabledAttempt(context: Context) {
        ensureChannel(context)

        postOrFallbackToast(
            context = context,
            notificationId = 703,
            title = "Accesos rápidos desactivados",
            text = "Actívalos en Ajustes para usar shortcuts.",
            addOpenSettingsAction = true
        )
    }

    fun notifyInfo(context: Context, notificationId: Int, title: String, text: String) {
        ensureChannel(context)

        postOrFallbackToast(
            context = context,
            notificationId = notificationId,
            title = title,
            text = text,
            addOpenSettingsAction = false
        )
    }

    /**
     * Regla de oro:
     * - Si PUEDE notificar -> SOLO notificación, CERO Toast.
     * - Si NO PUEDE por permiso (Android 13+) -> Toast fallback.
     * - Si el usuario apagó notificaciones en sistema -> no hacemos nada.
     */
    private fun postOrFallbackToast(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        addOpenSettingsAction: Boolean
    ) {
        // 1) Android 13+: si no hay permiso -> no hay notificación -> fallback Toast
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
                return
            }
        }

        val nm = NotificationManagerCompat.from(context)

        // 2) Si el usuario apagó notificaciones a nivel sistema -> respetamos y nos callamos
        if (!nm.areNotificationsEnabled()) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Tip pro: usa un icono MONOCROMO dedicado para notificaciones
            .setSmallIcon(R.drawable.ic_cloud_appsuite)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setTimeoutAfter(8000)
            .setContentIntent(mainPendingIntent(context))

//        if (addOpenSettingsAction) {
//            builder.addAction(
//                NotificationCompat.Action(
//                    0,
//                    "Abrir ajustes",
//                    settingsSystemPendingIntent(context)
//                )
//            )
//        }

        nm.notify(notificationId, builder.build())
    }

    private fun mainPendingIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, i, pendingFlags())
    }

    private fun settingsSystemPendingIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEPLINK_DEST, DEST_SETTINGS_SYSTEM)
            putExtra(EXTRA_SETTINGS_FOCUS, FOCUS_NOTIFICATIONS)
        }
        return PendingIntent.getActivity(context, 1, i, pendingFlags())
    }

    private fun pendingFlags(): Int {
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) base or PendingIntent.FLAG_IMMUTABLE else base
    }
}
