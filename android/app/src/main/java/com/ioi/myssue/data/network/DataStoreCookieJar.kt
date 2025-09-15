package com.ioi.myssue.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import kotlin.collections.map

class DataStoreCookieJar(
    private val dataStore: DataStore<Preferences>
) : CookieJar {

    companion object {
        private val COOKIE_KEY = stringSetPreferencesKey("Cookies")
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookieStrings = cookies.map { it.toString() }.toSet()
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { prefs ->
                prefs[COOKIE_KEY] = cookieStrings
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieStrings = runBlocking {
            dataStore.data.first()[COOKIE_KEY] ?: emptySet()
        }
        return cookieStrings.mapNotNull { Cookie.parse(url, it) }
    }
}
