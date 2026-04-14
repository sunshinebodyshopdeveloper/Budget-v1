package com.sunshine.appsuite.budget.settings.system.notifications.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.notificationSettingsDataStore by preferencesDataStore(name = "notification_settings")
