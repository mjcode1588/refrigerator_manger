package com.fridge.manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDataStore by preferencesDataStore(name = "notification_prefs")

data class NotificationSettings(
    val enabled: Boolean = true,
    val alertDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
    val includeExpired: Boolean = true,
)

@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyEnabled = booleanPreferencesKey("enabled")
    private val keyAlertDays = intPreferencesKey("alert_days")
    private val keyHour = intPreferencesKey("hour")
    private val keyMinute = intPreferencesKey("minute")
    private val keyIncludeExpired = booleanPreferencesKey("include_expired")
    private val keyLastNotifiedDate = stringPreferencesKey("last_notified_date")

    val settingsFlow: Flow<NotificationSettings> = context.notificationDataStore.data.map { prefs ->
        NotificationSettings(
            enabled = prefs[keyEnabled] ?: true,
            alertDays = prefs[keyAlertDays] ?: 3,
            hour = prefs[keyHour] ?: 9,
            minute = prefs[keyMinute] ?: 0,
            includeExpired = prefs[keyIncludeExpired] ?: true,
        )
    }

    suspend fun getSettings(): NotificationSettings = settingsFlow.first()

    suspend fun updateSettings(settings: NotificationSettings) {
        context.notificationDataStore.edit { prefs ->
            prefs[keyEnabled] = settings.enabled
            prefs[keyAlertDays] = settings.alertDays
            prefs[keyHour] = settings.hour
            prefs[keyMinute] = settings.minute
            prefs[keyIncludeExpired] = settings.includeExpired
        }
    }

    suspend fun getLastNotifiedDate(): String? =
        context.notificationDataStore.data.first()[keyLastNotifiedDate]

    suspend fun setLastNotifiedDate(dateIso: String) {
        context.notificationDataStore.edit { prefs ->
            prefs[keyLastNotifiedDate] = dateIso
        }
    }
}
