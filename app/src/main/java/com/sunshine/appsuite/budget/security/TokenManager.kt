// TokenManager.kt (Versión Final Unificada)
package com.sunshine.appsuite.budget.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            deleteCorruptedPrefs()
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deleteCorruptedPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
        // También limpiamos el compartido en el logout
        context.getSharedPreferences("shared_session_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrEmpty()

    fun getInheritedToken(): String? {
        return try {
            val mainAppContext = context.createPackageContext(
                "com.sunshine.appsuite",
                Context.CONTEXT_IGNORE_SECURITY
            )
            // Leemos el archivo NO cifrado que creamos en el paso 1
            val sharedPrefs = mainAppContext.getSharedPreferences("shared_session_prefs", Context.MODE_PRIVATE)
            sharedPrefs.getString(KEY_AUTH_TOKEN, null)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "sbs_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val MAIN_APP_PACKAGE = "com.sunshine.appsuite" // El ID de la app principal
    }
}