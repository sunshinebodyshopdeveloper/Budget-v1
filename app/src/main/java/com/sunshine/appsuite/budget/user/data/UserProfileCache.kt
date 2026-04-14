package com.sunshine.appsuite.budget.user.data

import android.content.Context
import com.sunshine.appsuite.budget.user.model.UserProfile

/**
 * Cache chiquito para evitar pantallas "vacías" al abrir la app.
 * Fuente de verdad sigue siendo la API + UserProfileStore.
 */
class UserProfileCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): UserProfile? {
        val hasAny = prefs.contains(KEY_NAME) || prefs.contains(KEY_EMAIL) || prefs.contains(KEY_USERNAME)
        if (!hasAny) return null

        return UserProfile(
            id = prefs.getInt(KEY_ID, -1).let { if (it == -1) null else it },
            name = prefs.getString(KEY_NAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            username = prefs.getString(KEY_USERNAME, null),
            phone = prefs.getString(KEY_PHONE, null),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putInt(KEY_ID, profile.id ?: -1)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_USERNAME, profile.username)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_AVATAR_URL, profile.avatarUrl)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "user_profile_cache"

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_USERNAME = "username"
        private const val KEY_PHONE = "phone"
        private const val KEY_AVATAR_URL = "avatar_url"
    }
}
