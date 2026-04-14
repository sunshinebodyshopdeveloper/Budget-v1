package com.sunshine.appsuite.budget.shortcuts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.sunshine.appsuite.budget.MainActivity
import com.sunshine.appsuite.budget.assistant.AssistantActivity
import com.sunshine.appsuite.budget.settings.SettingsActivity
import com.sunshine.appsuite.budget.settings.account.ProfilePhotoActivity
import com.sunshine.appsuite.budget.tools.qr.QrScannerActivity

object AppShortcuts {

    private const val SCHEME = "appsuite"
    private const val HOST = "shortcut"

    const val KEY_PHOTOS = "photos"
    const val KEY_QR = "qr"
    const val KEY_ASSISTANT = "assistant"
    const val KEY_SUPPORT = "support"

    fun extractKey(data: Uri?): String? {
        if (data == null) return null
        if (data.scheme != SCHEME) return null
        if (data.host != HOST) return null
        return data.pathSegments.firstOrNull() // photos | qr | support
    }

    fun open(activity: Activity, key: String?) {
        when (key) {

            KEY_PHOTOS -> {
                activity.startActivity(Intent(activity, ProfilePhotoActivity::class.java))
            }

            KEY_QR -> {
                activity.startActivity(Intent(activity, QrScannerActivity::class.java))
            }

            KEY_ASSISTANT -> {
                activity.startActivity(Intent(activity, AssistantActivity::class.java))
            }

            KEY_SUPPORT -> {
                SettingsActivity.Companion.start(activity, SettingsActivity.Section.SUPPORT)
            }

            else -> {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }
    }
}
