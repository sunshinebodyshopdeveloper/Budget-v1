package com.sunshine.appsuite.budget.user

import com.sunshine.appsuite.budget.BuildConfig
import com.sunshine.appsuite.budget.data.network.UserResponse
import com.sunshine.appsuite.budget.user.model.UserProfile

internal fun UserResponse.toUserProfile(): UserProfile {
    val avatarFromMedia = media
        ?.firstOrNull { !it.originalUrl.isNullOrBlank() }
        ?.originalUrl

    val rawAvatar = avatarFromMedia ?: avatar

    return UserProfile(
        id = id,
        name = name,
        email = email,
        username = username,
        phone = phone,
        avatarUrl = rawAvatar.absolutizeIfNeeded()
    )
}

private fun String?.absolutizeIfNeeded(): String? {
    val url = this?.trim().orEmpty()
    if (url.isBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    return BuildConfig.API_BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
}
