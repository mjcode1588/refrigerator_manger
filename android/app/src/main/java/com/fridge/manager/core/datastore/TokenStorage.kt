package com.fridge.manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyAccessToken = stringPreferencesKey("access_token")
    private val keyRefreshToken = stringPreferencesKey("refresh_token")
    private val keyHouseholdId = longPreferencesKey("current_household_id")
    private val keyNickname = stringPreferencesKey("user_nickname")

    val accessTokenFlow: Flow<String?> = context.authDataStore.data.map { it[keyAccessToken] }
    val currentHouseholdIdFlow: Flow<Long?> = context.authDataStore.data.map { it[keyHouseholdId] }
    val nicknameFlow: Flow<String?> = context.authDataStore.data.map { it[keyNickname] }

    suspend fun getAccessToken(): String? =
        context.authDataStore.data.first()[keyAccessToken]

    suspend fun getRefreshToken(): String? =
        context.authDataStore.data.first()[keyRefreshToken]

    suspend fun getCurrentHouseholdId(): Long? =
        context.authDataStore.data.first()[keyHouseholdId]

    suspend fun saveTokens(accessToken: String, refreshToken: String, nickname: String) {
        context.authDataStore.edit { prefs ->
            prefs[keyAccessToken] = accessToken
            prefs[keyRefreshToken] = refreshToken
            prefs[keyNickname] = nickname
        }
    }

    suspend fun saveCurrentHouseholdId(householdId: Long) {
        context.authDataStore.edit { prefs ->
            prefs[keyHouseholdId] = householdId
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
