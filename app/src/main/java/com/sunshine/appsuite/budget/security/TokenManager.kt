package com.sunshine.appsuite.budget.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun getToken(): String? =
        prefs.getString(KEY_AUTH_TOKEN, null)

    fun clearToken() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrEmpty()

    companion object {
        private const val PREFS_NAME = "sbs_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
