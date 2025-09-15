package com.ioi.myssue.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private fun <T> Preferences.Key<T>.flowIn(store: DataStore<Preferences>) =
    store.data.map { it[this] }

private suspend fun <T> Preferences.Key<T>.saveTo(store: DataStore<Preferences>, value: T) =
    store.edit { it[this] = value }

private suspend fun <T> Preferences.Key<T>.deleteFrom(store: DataStore<Preferences>) {
    store.edit { prefs ->
        prefs.remove(this)
    }
}

class AuthDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val USER_ID = longPreferencesKey("user_id")
        private val UUID = stringPreferencesKey("uuid")
    }

    val accessTokenFlow = ACCESS_TOKEN.flowIn(dataStore)
    val userIdFlow = USER_ID.flowIn(dataStore)
    val uuidFlow = UUID.flowIn(dataStore)

    suspend fun saveAccessToken(token: String) = ACCESS_TOKEN.saveTo(dataStore, token)
    suspend fun deleteAccessToken() = ACCESS_TOKEN.deleteFrom(dataStore)

    suspend fun saveUserId(id: Long) = USER_ID.saveTo(dataStore, id)
    suspend fun deleteUserId() = USER_ID.deleteFrom(dataStore)

    suspend fun saveUUID(uuid: String) = UUID.saveTo(dataStore, uuid)
    suspend fun deleteUUID() = UUID.deleteFrom(dataStore)
}
