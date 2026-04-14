package com.sunshine.appsuite.budget.settings.account

import android.content.Context

data class UserProfile(
    val name: String? = null,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val avatar: String? = null
)

class UserProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUserProfile(profile: UserProfile) {
        saveUserProfile(
            name = profile.name,
            email = profile.email,
            username = profile.username,
            phone = profile.phone,
            avatar = profile.avatar
        )
    }

    fun saveUserProfile(
        name: String?,
        email: String?,
        username: String? = null,
        phone: String? = null,
        avatar: String? = null
    ) {
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PHONE, phone)
            .putString(KEY_AVATAR, avatar)
            .apply()
    }

    fun getUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString(KEY_NAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            username = prefs.getString(KEY_USERNAME, null),
            phone = prefs.getString(KEY_PHONE, null),
            avatar = prefs.getString(KEY_AVATAR, null)
        )
    }

    // Helpers
    fun updateName(name: String?) = saveUserProfile(getUserProfile().copy(name = name))
    fun updateEmail(email: String?) = saveUserProfile(getUserProfile().copy(email = email))
    fun updateUsername(username: String?) = saveUserProfile(getUserProfile().copy(username = username))
    fun updatePhone(phone: String?) = saveUserProfile(getUserProfile().copy(phone = phone))
    fun updateAvatar(avatar: String?) = saveUserProfile(getUserProfile().copy(avatar = avatar))

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_USERNAME = "user_username"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_AVATAR = "user_avatar"
    }
}
