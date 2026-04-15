package com.sunshine.appsuite.budget.shortcuts

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import com.sunshine.appsuite.budget.R

object AppShortcutsToggle {

    private val IDS = listOf(
        AppShortcuts.KEY_PHOTOS,
        AppShortcuts.KEY_QR,
        AppShortcuts.KEY_ASSISTANT,
        AppShortcuts.KEY_SUPPORT
    )

    /**
     * - Si lo llamas desde el Application (arranque), NO notifica.
     * - Si lo llamas desde una Activity/Fragment (toggle del usuario), SÍ notifica.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val notify = context !is Application
        setEnabledInternal(context, enabled, notify)
    }

    /**
     * ✅ Úsalo cuando *quieras forzar* notificación sí o sí (por ejemplo, desde el switch).
     */
    fun setEnabledFromUser(context: Context, enabled: Boolean) {
        setEnabledInternal(context, enabled, notify = true)
    }

    private fun setEnabledInternal(context: Context, enabled: Boolean, notify: Boolean) {
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return

        runCatching {
            if (enabled) {
                // ✅ Publica dinámicos (y re-habilita si estaban disabled)
                sm.dynamicShortcuts = buildDynamicShortcuts(context)
                sm.enableShortcuts(IDS)
            } else {
                // ✅ Deshabilita (afecta también pinned) y quita dinámicos
                sm.disableShortcuts(IDS, "Desactivado por el usuario")
                sm.removeDynamicShortcuts(IDS)
            }
        }

        if (notify) {
            ShortcutsNotifier.notifyToggleChanged(context, enabled)
        }
    }

    private fun buildDynamicShortcuts(context: Context): List<ShortcutInfo> {
        return listOf(
            build(
                context = context,
                id = AppShortcuts.KEY_PHOTOS,
                shortLabelRes = R.string.shortcut_photos_long,
                longLabelRes = R.string.shortcut_photos_long,
                iconRes = R.drawable.ic_shortcut_camera,
                uri = "appsuite://shortcut/photos",
                rank = 0
            ),
            build(
                context = context,
                id = AppShortcuts.KEY_QR,
                shortLabelRes = R.string.shortcut_qr,
                longLabelRes = R.string.shortcut_qr_long,
                iconRes = R.drawable.ic_shortcut_qr,
                uri = "appsuite://shortcut/qr",
                rank = 1
            ),
            build(
                context = context,
                id = AppShortcuts.KEY_ASSISTANT,
                shortLabelRes = R.string.shortcut_assistant,
                longLabelRes = R.string.shortcut_assistant_long,
                iconRes = R.drawable.ic_shortcut_assistant,
                uri = "appsuite://shortcut/assistant",
                rank = 2
            ),
            build(
                context = context,
                id = AppShortcuts.KEY_SUPPORT,
                shortLabelRes = R.string.shortcut_support,
                longLabelRes = R.string.shortcut_support_long,
                iconRes = R.drawable.ic_shortcut_help,
                uri = "appsuite://shortcut/support",
                rank = 3
            )
        )
    }

    private fun build(
        context: Context,
        id: String,
        shortLabelRes: Int,
        longLabelRes: Int,
        iconRes: Int,
        uri: String,
        rank: Int
    ): ShortcutInfo {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            setClass(context, ShortcutRouterActivity::class.java)
        }

        return ShortcutInfo.Builder(context, id)
            .setShortLabel(context.getString(shortLabelRes))
            .setLongLabel(context.getString(longLabelRes))
            .setIcon(Icon.createWithResource(context, iconRes))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }
}
