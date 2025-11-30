package com.example.dailyinsight.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.dailyinsight.data.datastore.CookieKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ProxyCookieJar(
    private val real: MyCookieJar,
    private val dataStore: DataStore<Preferences>,
) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // 1) RealCookieJar 에 저장
        real.saveFromResponse(url, cookies)

        // 2) DataStore 에도 저장 (persistent)
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { prefs ->
                cookies.forEach { cookie ->
                    prefs[stringPreferencesKey(cookie.name)] = cookie.value
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 1) 메모리 먼저 확인
        val memCookies = real.loadForRequest(url)
        if (memCookies.isNotEmpty()) return memCookies

        // 2) 메모리에 없으면 DataStore 에서 로드 (lazy load)
        val prefs = runBlocking { dataStore.data.first() }

        val accessToken = prefs[stringPreferencesKey("access_token")]
        val refreshToken = prefs[stringPreferencesKey("refresh_token")]
        val csrfToken = prefs[CookieKeys.CSRF_TOKEN]

        val cookies = mutableListOf<Cookie>()

        accessToken?.let {
            cookies.add(
                Cookie.Builder()
                    .name("access_token")
                    .value(it)
                    .domain(url.host)
                    .path("/")
                    .httpOnly()
                    .secure()
                    .build()
            )
        }

        refreshToken?.let {
            cookies.add(
                Cookie.Builder()
                    .name("refresh_token")
                    .value(it)
                    .domain(url.host)
                    .path("/")
                    .httpOnly()
                    .secure()
                    .build()
            )
        }

        csrfToken?.let {
            cookies.add(
                Cookie.Builder()
                    .name("csrftoken")
                    .value(it)
                    .domain(url.host)
                    .path("/")
                    .httpOnly()
                    .secure()
                    .build()
            )
        }

        if (cookies.isNotEmpty()) {
            real.saveFromResponse(url, cookies)
        }

        return cookies
    }

    fun clear() {
        real.clear()

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { it.clear() }
        }
    }
}