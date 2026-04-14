package com.sunshine.appsuite.budget.settings.system.verifications

enum class SystemCheckId {
    INTERNET,
    SERVER,
    APP_SECURITY,
    NOTIFICATIONS,
    DEVICE_TIME,
    SYNC,
    STORAGE
}

enum class SystemCheckState { IDLE, RUNNING, OK, WARN, ERROR }

sealed class SystemCheckAction {
    object EnableAppLock : SystemCheckAction()
    object EnableAutoTime : SystemCheckAction()
}

data class SystemCheckUi(
    val id: SystemCheckId,
    val title: String,
    val state: SystemCheckState = SystemCheckState.IDLE,
    val subtitle: String,
    val meta: String,
    val action: SystemCheckAction? = null,
    val actionLabel: String? = null
)

data class SystemCheckResult(
    val state: SystemCheckState,
    val subtitle: String,
    val action: SystemCheckAction? = null,
    val actionLabel: String? = null
)
