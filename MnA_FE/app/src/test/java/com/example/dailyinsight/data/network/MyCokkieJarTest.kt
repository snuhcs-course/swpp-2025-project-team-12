package com.example.dailyinsight.data.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MyCookieJarTest {

    private lateinit var cookieJar: MyCookieJar
    private val testUrl = "https://example.com/api".toHttpUrl()
    private val otherUrl = "https://other.com/api".toHttpUrl()

    @Before
    fun setup() {
        cookieJar = MyCookieJar()
    }

    // ===== saveFromResponse Tests =====

    @Test
    fun saveFromResponse_storesCookiesForHost() {
        val cookies = listOf(
            createCookie("access_token", "abc123"),
            createCookie("refresh_token", "xyz789")
        )

        cookieJar.saveFromResponse(testUrl, cookies)
        val loaded = cookieJar.loadForRequest(testUrl)

        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.name == "access_token" && it.value == "abc123" })
        assertTrue(loaded.any { it.name == "refresh_token" && it.value == "xyz789" })
    }

    @Test
    fun saveFromResponse_overwritesPreviousCookies() {
        val oldCookies = listOf(createCookie("token", "old_value"))
        val newCookies = listOf(createCookie("token", "new_value"))

        cookieJar.saveFromResponse(testUrl, oldCookies)
        cookieJar.saveFromResponse(testUrl, newCookies)
        val loaded = cookieJar.loadForRequest(testUrl)

        assertEquals(1, loaded.size)
        assertEquals("new_value", loaded[0].value)
    }

    @Test
    fun saveFromResponse_separatesByHost() {
        val cookies1 = listOf(createCookie("token", "value1"))
        val cookies2 = listOf(createCookie("token", "value2"))

        cookieJar.saveFromResponse(testUrl, cookies1)
        cookieJar.saveFromResponse(otherUrl, cookies2)

        val loaded1 = cookieJar.loadForRequest(testUrl)
        val loaded2 = cookieJar.loadForRequest(otherUrl)

        assertEquals("value1", loaded1[0].value)
        assertEquals("value2", loaded2[0].value)
    }

    // ===== loadForRequest Tests =====

    @Test
    fun loadForRequest_returnsEmptyListWhenNoCookies() {
        val loaded = cookieJar.loadForRequest(testUrl)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun loadForRequest_returnsEmptyListForUnknownHost() {
        val cookies = listOf(createCookie("token", "value"))
        cookieJar.saveFromResponse(testUrl, cookies)

        val loaded = cookieJar.loadForRequest(otherUrl)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun loadForRequest_returnsCookiesForCorrectHost() {
        val cookies = listOf(
            createCookie("session", "session123"),
            createCookie("user", "user456")
        )
        cookieJar.saveFromResponse(testUrl, cookies)

        val loaded = cookieJar.loadForRequest(testUrl)

        assertEquals(2, loaded.size)
    }

    // ===== clear Tests =====

    @Test
    fun clear_removesAllCookies() {
        val cookies = listOf(createCookie("token", "value"))
        cookieJar.saveFromResponse(testUrl, cookies)
        cookieJar.saveFromResponse(otherUrl, cookies)

        cookieJar.clear()

        assertTrue(cookieJar.loadForRequest(testUrl).isEmpty())
        assertTrue(cookieJar.loadForRequest(otherUrl).isEmpty())
    }

    @Test
    fun clear_allowsNewCookiesAfterClear() {
        val oldCookies = listOf(createCookie("old", "old_value"))
        cookieJar.saveFromResponse(testUrl, oldCookies)
        cookieJar.clear()

        val newCookies = listOf(createCookie("new", "new_value"))
        cookieJar.saveFromResponse(testUrl, newCookies)
        val loaded = cookieJar.loadForRequest(testUrl)

        assertEquals(1, loaded.size)
        assertEquals("new", loaded[0].name)
    }

    // ===== Edge Cases =====

    @Test
    fun saveFromResponse_handlesEmptyCookieList() {
        cookieJar.saveFromResponse(testUrl, emptyList())
        val loaded = cookieJar.loadForRequest(testUrl)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun saveFromResponse_handlesSingleCookie() {
        val cookies = listOf(createCookie("single", "value"))

        cookieJar.saveFromResponse(testUrl, cookies)
        val loaded = cookieJar.loadForRequest(testUrl)

        assertEquals(1, loaded.size)
        assertEquals("single", loaded[0].name)
    }

    @Test
    fun loadForRequest_worksWithDifferentPaths() {
        val url1 = "https://example.com/api/v1".toHttpUrl()
        val url2 = "https://example.com/api/v2".toHttpUrl()
        val cookies = listOf(createCookie("token", "value"))

        cookieJar.saveFromResponse(url1, cookies)
        val loaded = cookieJar.loadForRequest(url2)

        // Same host, should return cookies
        assertEquals(1, loaded.size)
    }

    // ===== Helper =====

    private fun createCookie(name: String, value: String): Cookie {
        return Cookie.Builder()
            .name(name)
            .value(value)
            .domain("example.com")
            .path("/")
            .build()
    }
}