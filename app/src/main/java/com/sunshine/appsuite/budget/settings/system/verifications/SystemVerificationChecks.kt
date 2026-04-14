package com.sunshine.appsuite.budget.settings.system.verifications

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.sunshine.appsuite.budget.settings.security.SecurityPreferences
import com.sunshine.appsuite.budget.settings.system.notifications.data.NotificationSettingsRepository
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SystemVerificationChecks {

    data class ServerEvaluation(
        val result: SystemCheckResult,
        val latencyMs: Long? = null
    )

    /**
     * Ping ligero al server (default: /api/test).
     * Regla práctica: si el server responde con CUALQUIER HTTP code, cuenta como “responde”.
     * (Si quieres /health real, cambia el path y/o la regla.)
     */
    suspend fun checkServerAppSuite(
        baseUrl: String,
        path: String = "api/test",
        warnSlowMs: Long = 1200L
    ): ServerEvaluation = withContext(Dispatchers.IO) {
        val url = joinUrl(baseUrl, path)

        try {
            val start = SystemClock.elapsedRealtime()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("Accept", "application/json")
            }

            // Con que exista responseCode, el server ya respondió (aunque sea 401/404).
            val _code = conn.responseCode
            conn.disconnect()

            val ms = SystemClock.elapsedRealtime() - start

            val state = if (ms >= warnSlowMs) SystemCheckState.WARN else SystemCheckState.OK
            val subtitle = if (state == SystemCheckState.WARN) "Lento" else "Responde"

            ServerEvaluation(
                result = SystemCheckResult(state = state, subtitle = subtitle),
                latencyMs = ms
            )
        } catch (_: Exception) {
            ServerEvaluation(
                result = SystemCheckResult(SystemCheckState.ERROR, "No responde"),
                latencyMs = null
            )
        }
    }

    fun checkInternet(context: Context): SystemCheckResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return SystemCheckResult(SystemCheckState.ERROR, "Sin conexión")
        val caps = cm.getNetworkCapabilities(network) ?: return SystemCheckResult(SystemCheckState.ERROR, "Sin conexión")

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return SystemCheckResult(SystemCheckState.ERROR, "Sin conexión")

        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (validated) {
            SystemCheckResult(SystemCheckState.OK, "Validada")
        } else {
            SystemCheckResult(SystemCheckState.WARN, "Conectado, sin salida")
        }
    }

    fun checkAppSecurity(context: Context): SystemCheckResult {
        val pinEnabled = SecurityPreferences.isPinEnabled(context)
        val bioEnabled = SecurityPreferences.isBiometricEnabled(context)

        // Info extra (por si luego quieres enriquecer):
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val _deviceSecure = km.isDeviceSecure

        val hasLock = pinEnabled || bioEnabled

        return if (hasLock) {
            SystemCheckResult(SystemCheckState.OK, "Huella/PIN activo")
        } else {
            SystemCheckResult(
                state = SystemCheckState.WARN,
                subtitle = "Sin bloqueo configurado",
                action = SystemCheckAction.EnableAppLock,
                actionLabel = "Activar"
            )
        }
    }

    /**
     * Notificaciones:
     * - ERROR si están bloqueadas a nivel sistema (o permiso en Android 13+).
     * - WARN si en AppSuite el usuario apagó todos los topics (“silenciadas”).
     * - OK si todo bien.
     */
    suspend fun checkNotifications(
        context: Context,
        repo: NotificationSettingsRepository
    ): SystemCheckResult = withContext(Dispatchers.Default) {

        val nm = NotificationManagerCompat.from(context)

        val blockedAppLevel = !nm.areNotificationsEnabled()
        val blockedPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

        if (blockedAppLevel || blockedPermission) {
            return@withContext SystemCheckResult(SystemCheckState.ERROR, "Bloqueadas")
        }

        // AppSuite-level (“silenciadas” si el usuario apagó TODO)
        return@withContext try {
            val map = repo.settingsFlow.first()
            val anyEnabled = map.values.any { it }
            if (!anyEnabled) SystemCheckResult(SystemCheckState.WARN, "Silenciadas")
            else SystemCheckResult(SystemCheckState.OK, "Habilitadas")
        } catch (_: Exception) {
            // Si por algo falla DataStore, no castigamos al usuario.
            SystemCheckResult(SystemCheckState.OK, "Habilitadas")
        }
    }

    fun checkDeviceTime(context: Context): SystemCheckResult {
        val autoTime = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        val autoZone = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 0) == 1

        return if (autoTime && autoZone) {
            SystemCheckResult(SystemCheckState.OK, "Automática")
        } else {
            SystemCheckResult(
                state = SystemCheckState.WARN,
                subtitle = "Manual",
                action = SystemCheckAction.EnableAutoTime,
                actionLabel = "Activar automático"
            )
        }
    }

    /**
     * Sync real (si ya tienes WorkManager corriendo sync jobs con TAG).
     */
    suspend fun checkSyncWorkManager(context: Context, tag: String): SystemCheckResult {
        return try {
            val infos = WorkManager.getInstance(context).getWorkInfosByTag(tag).await()

            when {
                infos.any { it.state == WorkInfo.State.FAILED } ->
                    SystemCheckResult(SystemCheckState.ERROR, "Falló el último intento")

                infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } ->
                    SystemCheckResult(SystemCheckState.WARN, "Pendiente")

                else ->
                    SystemCheckResult(SystemCheckState.OK, "Al día")
            }
        } catch (_: Exception) {
            SystemCheckResult(SystemCheckState.WARN, "Pendiente")
        }
    }

    /**
     * Sync fallback cuando aún no hay WorkManager:
     * - Si internet+server OK → “Al día”
     * - Si internet OK pero server ERROR → “Falló el último intento”
     * - Lo demás → “Pendiente”
     */
    fun checkSyncFallback(internetState: SystemCheckState, serverState: SystemCheckState): SystemCheckResult {
        return when {
            internetState == SystemCheckState.OK && serverState == SystemCheckState.OK ->
                SystemCheckResult(SystemCheckState.OK, "Al día")

            internetState == SystemCheckState.OK && serverState == SystemCheckState.ERROR ->
                SystemCheckResult(SystemCheckState.ERROR, "Falló el último intento")

            else ->
                SystemCheckResult(SystemCheckState.WARN, "Pendiente")
        }
    }

    fun checkStorage(context: Context): SystemCheckResult {
        val fs = StatFs(context.filesDir.absolutePath)
        val freeBytes = fs.availableBytes

        val warnThreshold = 500L * 1024L * 1024L  // 500 MB
        val errThreshold = 200L * 1024L * 1024L   // 200 MB

        return when {
            freeBytes <= errThreshold -> SystemCheckResult(SystemCheckState.ERROR, "Crítico")
            freeBytes <= warnThreshold -> SystemCheckResult(SystemCheckState.WARN, "Bajo")
            else -> SystemCheckResult(SystemCheckState.OK, "Suficiente")
        }
    }

    private fun joinUrl(baseUrl: String, path: String): String {
        val b = baseUrl.trim()
        val left = b.trimEnd('/')
        val right = path.trim().trimStart('/')
        return "$left/$right"
    }
}

// Mini await para ListenableFuture sin depender de coroutines-guava
private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        val executor = Executors.newSingleThreadExecutor()
        addListener({
            try { cont.resume(get()) }
            catch (e: Exception) { cont.resumeWithException(e) }
            finally { executor.shutdown() }
        }, executor)
    }
