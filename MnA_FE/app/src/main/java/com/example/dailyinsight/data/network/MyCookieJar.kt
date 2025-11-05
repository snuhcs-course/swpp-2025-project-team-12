package com.example.dailyinsight.data.network

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import kotlin.collections.joinToString

class MyCookieJar(private val context : Context) : CookieJar {
    private val cookies: MutableMap<String, List<Cookie>> = mutableMapOf()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        Log.i("MyCookieJar", "request before: " + (cookies[url.host] ?: emptyList()).joinToString())
        if (cookies[url.host].isNullOrEmpty()) {
            val cookieStored = runBlocking { context.cookieDataStore.data.first() }
            val accessToken = cookieStored[CookieKeys.ACCESS_TOKEN]
            val refreshToken = cookieStored[CookieKeys.REFRESH_TOKEN]
            val cookies = mutableListOf<Cookie>()

            accessToken?.let {
                cookies.add(
                    Cookie.Builder()
                        .name("access_token")
                        .value(it)
                        .domain(url.host)
                        .path("/")
                        .secure()
                        .httpOnly()
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
                        .secure()
                        .httpOnly()
                        .build()
                )
            }
            this.cookies[url.host] = cookies
        }

        Log.i("MyCookieJar", "request final: " + (cookies[url.host] ?: emptyList()).joinToString())
        return this.cookies[url.host] ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Log.i("MyCookieJar", "response came: " + cookies.joinToString())
        this.cookies[url.host] = cookies
        saveTokensToDataStore(cookies)
    }

    private fun saveTokensToDataStore(cookies: List<Cookie>) {
        val accessToken = cookies.first { it.name == "access_token" }.value
        val refreshToken = cookies.first { it.name == "refresh_token" }.value
        CoroutineScope(Dispatchers.IO).launch {
            context.cookieDataStore.edit { prefs ->
                prefs[CookieKeys.ACCESS_TOKEN] = accessToken
            }
            context.cookieDataStore.edit { prefs ->
                prefs[CookieKeys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    private fun cleanDataStore() {
        // wipe all cookies
        // never used here
        // needed for logout later
        runBlocking {
            context.cookieDataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }
}