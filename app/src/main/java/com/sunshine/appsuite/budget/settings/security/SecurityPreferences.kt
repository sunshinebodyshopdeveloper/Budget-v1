package com.sunshine.appsuite.budget.settings.security

import android.content.Context
import androidx.core.content.edit

object SecurityPreferences {

    private const val PREFS_NAME = "security_prefs"
    private const val KEY_PIN = "security_pin"
    private const val KEY_PIN_ENABLED = "security_pin_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "security_biometric_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Guarda el PIN y lo marca como habilitado.
     * IMPORTANTE: desactiva huella para que nunca estén las dos activas.
     */
    fun savePin(context: Context, pin: String) {
        prefs(context).edit {
            putString(KEY_PIN, pin)
            putBoolean(KEY_PIN_ENABLED, true)
            // Regla: si se registra PIN, se desactiva huella
            putBoolean(KEY_BIOMETRIC_ENABLED, false)
        }
    }

    fun getPin(context: Context): String? =
        prefs(context).getString(KEY_PIN, null)

    /**
     * Limpia el PIN y lo deja deshabilitado.
     */
    fun clearPin(context: Context) {
        prefs(context).edit {
            remove(KEY_PIN)
            putBoolean(KEY_PIN_ENABLED, false)
        }
    }

    /**
     * Habilita o deshabilita el uso de PIN.
     * Si se habilita, desactiva huella (solo un método activo).
     */
    fun setPinEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_PIN_ENABLED, enabled)
            if (enabled) {
                // Si el PIN se activa manualmente, desactivamos huella
                putBoolean(KEY_BIOMETRIC_ENABLED, false)
            }
        }
    }

    fun isPinEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PIN_ENABLED, false) &&
                !getPin(context).isNullOrEmpty()

    /**
     * Habilita o deshabilita huella.
     * Si se habilita, desactiva PIN y borra el valor guardado.
     */
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            if (enabled) {
                // Si se activa huella, se desactiva PIN y se limpia
                putBoolean(KEY_PIN_ENABLED, false)
                remove(KEY_PIN)
            }
        }
    }

    fun isBiometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
}
