package com.sunshine.appsuite.budget.settings.system.verifications

import android.content.Context
import com.sunshine.appsuite.budget.settings.system.notifications.data.NotificationSettingsRepository
import kotlinx.coroutines.delay

/**
 * Orquesta la ejecución de los checks y emite updates para que la UI se pinte con loader → resultado.
 * La idea: SettingsSystemFragment NO piensa, solo pinta.
 */
class SystemVerificationRunner(
    private val context: Context,
    private val notificationRepo: NotificationSettingsRepository,
    private val baseUrl: String,
    private val syncWorkTag: String? = null,
    private val serverPath: String = "api/test",
) {

    data class Update(
        val id: SystemCheckId,
        val state: SystemCheckState,
        val subtitle: String,
        val meta: String = "",
        val action: SystemCheckAction? = null,
        val actionLabel: String? = null
    )

    fun initialItems(): List<SystemCheckUi> = listOf(
        SystemCheckUi(SystemCheckId.INTERNET,       "Conexión a internet",  SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.SERVER,         "Servidor AppSuite",    SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.APP_SECURITY,   "Seguridad de la app",  SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.NOTIFICATIONS,  "Notificaciones",       SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.DEVICE_TIME,    "Hora del dispositivo", SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.SYNC,           "Sincronización",       SystemCheckState.IDLE, "Listo para verificar", "", null, null),
        SystemCheckUi(SystemCheckId.STORAGE,        "Almacenamiento",       SystemCheckState.IDLE, "Listo para verificar", "", null, null),
    )

    /**
     * Ejecuta los 7 checks en orden.
     * [emit] se llama varias veces: primero RUNNING, luego OK/WARN/ERROR.
     */
    suspend fun runAll(emit: (Update) -> Unit) {
        // 1) Internet
        emitRunning(emit, SystemCheckId.INTERNET)
        delay(180)
        val internet = SystemVerificationChecks.checkInternet(context)
        emitResult(emit, SystemCheckId.INTERNET, internet)

        // 2) Server (ping)
        emitRunning(emit, SystemCheckId.SERVER)
        delay(180)
        val serverEval = SystemVerificationChecks.checkServerAppSuite(
            baseUrl = baseUrl,
            path = serverPath
        )
        val serverMeta = serverEval.latencyMs?.let { "${it} ms" } ?: ""
        emitResult(emit, SystemCheckId.SERVER, serverEval.result, meta = serverMeta)

        // 3) App security
        emitRunning(emit, SystemCheckId.APP_SECURITY)
        delay(180)
        val security = SystemVerificationChecks.checkAppSecurity(context)
        emitResult(emit, SystemCheckId.APP_SECURITY, security)

        // 4) Notifications (Android-level + AppSuite-level)
        emitRunning(emit, SystemCheckId.NOTIFICATIONS)
        delay(180)
        val notifs = SystemVerificationChecks.checkNotifications(context, notificationRepo)
        emitResult(emit, SystemCheckId.NOTIFICATIONS, notifs)

        // 5) Device time
        emitRunning(emit, SystemCheckId.DEVICE_TIME)
        delay(180)
        val time = SystemVerificationChecks.checkDeviceTime(context)
        emitResult(emit, SystemCheckId.DEVICE_TIME, time)

        // 6) Sync
        emitRunning(emit, SystemCheckId.SYNC)
        delay(180)
        val sync = if (!syncWorkTag.isNullOrBlank()) {
            SystemVerificationChecks.checkSyncWorkManager(context, syncWorkTag)
        } else {
            SystemVerificationChecks.checkSyncFallback(
                internetState = internet.state,
                serverState = serverEval.result.state
            )
        }
        emitResult(emit, SystemCheckId.SYNC, sync)

        // 7) Storage
        emitRunning(emit, SystemCheckId.STORAGE)
        delay(180)
        val storage = SystemVerificationChecks.checkStorage(context)
        emitResult(emit, SystemCheckId.STORAGE, storage)
    }

    private fun emitRunning(emit: (Update) -> Unit, id: SystemCheckId) {
        emit(
            Update(
                id = id,
                state = SystemCheckState.RUNNING,
                subtitle = "Verificando…",
                meta = ""
            )
        )
    }

    private fun emitResult(
        emit: (Update) -> Unit,
        id: SystemCheckId,
        result: SystemCheckResult,
        meta: String = ""
    ) {
        emit(
            Update(
                id = id,
                state = result.state,
                subtitle = result.subtitle,
                meta = meta,
                action = result.action,
                actionLabel = result.actionLabel
            )
        )
    }
}
