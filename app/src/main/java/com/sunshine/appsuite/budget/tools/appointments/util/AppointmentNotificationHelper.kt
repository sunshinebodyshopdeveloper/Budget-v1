package com.sunshine.appsuite.budget.tools.appointments.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.settings.system.notifications.data.notificationSettingsDataStore
import com.sunshine.appsuite.budget.settings.system.notifications.model.NotificationTopic
import com.sunshine.appsuite.budget.tools.appointments.AppointmentsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppointmentNotificationHelper {

    // Canal (Android 8+)
    const val CHANNEL_ID = "appointments_created"

    // Extra para deep-link a detalle
    const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"

    /**
     * Crea la notificación "Nueva cita creada".
     *
     * Respeta:
     * 1) Permisos / ajustes del sistema (Android)
     * 2) Tu switch interno de Settings (DataStore) para el topic APPOINTMENT_NEW
     */
    fun showAppointmentCreated(
        context: Context,
        appointmentId: Long?,
        contentText: String
    ) {
        // ⚠️ No bloqueamos el hilo (CreateAppointment suele estar en Main)
        val appCtx = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            if (!isAppointmentTopicEnabled(appCtx)) return@launch
            if (!canPostSystemNotifications(appCtx)) return@launch

            ensureChannel(appCtx)

            val openIntent = Intent(appCtx, AppointmentsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (appointmentId != null && appointmentId > 0L) {
                    putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
                }
            }

            val openPendingIntent = PendingIntent.getActivity(
                appCtx,
                (appointmentId?.coerceAtLeast(0L)?.rem(Int.MAX_VALUE.toLong()) ?: 0L).toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notifId = ((appointmentId ?: System.currentTimeMillis())
                .rem(Int.MAX_VALUE.toLong())).toInt()

            val builder = NotificationCompat.Builder(appCtx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_appsuite)
                .setContentTitle(appCtx.getString(R.string.appointments_notification_created_title))
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    appCtx.getString(R.string.appointments_notification_action_view),
                    openPendingIntent
                )

            NotificationManagerCompat.from(appCtx).notify(notifId, builder.build())
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channelName = context.getString(R.string.appointments_notification_channel_name)
        val channelDesc = context.getString(R.string.appointments_notification_channel_desc)

        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = channelDesc
        }

        manager.createNotificationChannel(channel)
    }

    private fun canPostSystemNotifications(context: Context): Boolean {
        // Android 13+ requiere permiso runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private suspend fun isAppointmentTopicEnabled(context: Context): Boolean {
        return runCatching {
            val prefs = context.notificationSettingsDataStore.data.first()
            val key = booleanPreferencesKey(NotificationTopic.APPOINTMENT_NEW.key)
            // Default: true (si el usuario no tocó nada, SÍ notificamos)
            prefs[key] ?: true
        }.getOrElse {
            // Si algo raro pasa con DataStore, mejor no bloquear notificaciones
            true
        }
    }
}
