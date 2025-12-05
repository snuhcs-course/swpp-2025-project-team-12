package com.example.dailyinsight.data.network

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class MyCookieJar : CookieJar {
    private val cookies: MutableMap<String, List<Cookie>> = mutableMapOf()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        Log.i("MyCookieJar", "request with: " + (cookies[url.host] ?: emptyList()).joinToString())
        return this.cookies[url.host] ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Log.i("MyCookieJar", "response came: " + cookies.joinToString())
        this.cookies[url.host] = cookies
    }

    fun clear() {
        cookies.clear()
    }
}