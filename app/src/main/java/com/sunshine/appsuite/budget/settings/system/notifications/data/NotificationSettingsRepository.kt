package com.sunshine.appsuite.budget.settings.system.notifications.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sunshine.appsuite.budget.settings.system.notifications.model.NotificationTopic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationSettingsRepository(private val context: Context) {

    private fun keyOf(topic: NotificationTopic) = booleanPreferencesKey(topic.key)

    val settingsFlow: Flow<Map<NotificationTopic, Boolean>> =
        context.notificationSettingsDataStore.data.map { prefs ->
            NotificationTopic.entries.associateWith { topic ->
                // Default: true (o sea, si el usuario no ha tocado nada, sí notificas)
                prefs[keyOf(topic)] ?: true
            }
        }

    suspend fun setEnabled(topic: NotificationTopic, enabled: Boolean) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[keyOf(topic)] = enabled
        }
    }

    suspend fun setAll(enabled: Boolean) {
        context.notificationSettingsDataStore.edit { prefs ->
            NotificationTopic.entries.forEach { topic ->
                prefs[keyOf(topic)] = enabled
            }
        }
    }
}
