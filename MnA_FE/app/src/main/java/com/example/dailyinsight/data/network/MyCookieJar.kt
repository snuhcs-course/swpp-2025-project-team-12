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

class MyCookieJar : CookieJar {
    private val cookies: MutableMap<String, List<Cookie>> = mutableMapOf()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        Log.i("MyCookieJar", "request with: " + (cookies[url.host] ?: emptyList()).joinToString())
        return this.cookies[url.host] ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Log.i("MyCookieJar", "response came: " + cookies.joinToString())
        this.cookies[url.host] = cookies
//        saveTokensToDataStore(cookies)
    }

//    private fun saveTokensToDataStore(cookies: List<Cookie>) {
//        val accessToken = cookies.first { it.name == "access_token" }.value
//        val refreshToken = cookies.first { it.name == "refresh_token" }.value
//        CoroutineScope(Dispatchers.IO).launch {
//            context.cookieDataStore.edit { prefs ->
//                prefs[CookieKeys.ACCESS_TOKEN] = accessToken
//            }
//            context.cookieDataStore.edit { prefs ->
//                prefs[CookieKeys.REFRESH_TOKEN] = refreshToken
//            }
//        }
//    }

    // null-safe version
//    private fun saveTokensToDataStore(cookies: List<Cookie>) {
//        val accessToken = cookies.find { it.name == "access_token" }?.value
//        val refreshToken = cookies.find { it.name == "refresh_token" }?.value
//
//        if (accessToken == null && refreshToken == null) return // 저장할 토큰 없으면 패스
//
//        CoroutineScope(Dispatchers.IO).launch {
//            context.cookieDataStore.edit { prefs ->
//                accessToken?.let { prefs[CookieKeys.ACCESS_TOKEN] = it }
//                refreshToken?.let { prefs[CookieKeys.REFRESH_TOKEN] = it }
//            }
//        }
//    }

    fun clear() {
        cookies.clear()
    }
}