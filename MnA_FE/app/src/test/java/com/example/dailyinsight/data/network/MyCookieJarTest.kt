package com.example.dailyinsight.data.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MyCookieJarTest {

    private lateinit var cookieJar: MyCookieJar

    @Before
    fun setup() {
        cookieJar = MyCookieJar()
    }

    // ===== loadForRequest Tests =====

    @Test
    fun loadForRequest_emptyJar_returnsEmptyList() {
        val url = "https://example.com/api".toHttpUrl()

        val cookies = cookieJar.loadForRequest(url)

        assertTrue(cookies.isEmpty())
    }

    @Test
    fun loadForRequest_afterSave_returnsSavedCookies() {
        val url = "https://example.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url, listOf(cookie))
        val loaded = cookieJar.loadForRequest(url)

        assertEquals(1, loaded.size)
        assertEquals("session", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun loadForRequest_differentHost_returnsEmptyList() {
        val url1 = "https://example.com/api".toHttpUrl()
        val url2 = "https://other.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url1, listOf(cookie))
        val loaded = cookieJar.loadForRequest(url2)

        assertTrue(loaded.isEmpty())
    }

    // ===== saveFromResponse Tests =====

    @Test
    fun saveFromResponse_storesCookies() {
        val url = "https://example.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("token")
            .value("xyz789")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url, listOf(cookie))
        val loaded = cookieJar.loadForRequest(url)

        assertEquals(1, loaded.size)
    }

    @Test
    fun saveFromResponse_multipleCookies() {
        val url = "https://example.com/api".toHttpUrl()
        val cookie1 = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .path("/")
            .build()
        val cookie2 = Cookie.Builder()
            .name("token")
            .value("xyz789")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url, listOf(cookie1, cookie2))
        val loaded = cookieJar.loadForRequest(url)

        assertEquals(2, loaded.size)
    }

    @Test
    fun saveFromResponse_replacesCookiesForSameHost() {
        val url = "https://example.com/api".toHttpUrl()
        val cookie1 = Cookie.Builder()
            .name("old")
            .value("value1")
            .domain("example.com")
            .path("/")
            .build()
        val cookie2 = Cookie.Builder()
            .name("new")
            .value("value2")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url, listOf(cookie1))
        cookieJar.saveFromResponse(url, listOf(cookie2))
        val loaded = cookieJar.loadForRequest(url)

        // Should only have the new cookie (replaces entire list)
        assertEquals(1, loaded.size)
        assertEquals("new", loaded[0].name)
    }

    @Test
    fun saveFromResponse_emptyList() {
        val url = "https://example.com/api".toHttpUrl()

        cookieJar.saveFromResponse(url, emptyList())
        val loaded = cookieJar.loadForRequest(url)

        assertTrue(loaded.isEmpty())
    }

    // ===== clear Tests =====

    @Test
    fun clear_removesAllCookies() {
        val url = "https://example.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url, listOf(cookie))
        cookieJar.clear()
        val loaded = cookieJar.loadForRequest(url)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun clear_multipleHosts() {
        val url1 = "https://example.com/api".toHttpUrl()
        val url2 = "https://other.com/api".toHttpUrl()
        val cookie1 = Cookie.Builder()
            .name("session1")
            .value("abc")
            .domain("example.com")
            .path("/")
            .build()
        val cookie2 = Cookie.Builder()
            .name("session2")
            .value("xyz")
            .domain("other.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url1, listOf(cookie1))
        cookieJar.saveFromResponse(url2, listOf(cookie2))
        cookieJar.clear()

        assertTrue(cookieJar.loadForRequest(url1).isEmpty())
        assertTrue(cookieJar.loadForRequest(url2).isEmpty())
    }

    @Test
    fun clear_onEmptyJar_noError() {
        // Should not throw
        cookieJar.clear()
        assertTrue(cookieJar.loadForRequest("https://example.com".toHttpUrl()).isEmpty())
    }

    // ===== Edge Cases =====

    @Test
    fun cookieJar_handlesSubdomains() {
        val url1 = "https://api.example.com/test".toHttpUrl()
        val url2 = "https://www.example.com/test".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc")
            .domain("api.example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url1, listOf(cookie))

        // Different subdomain should not get the cookie
        assertTrue(cookieJar.loadForRequest(url2).isEmpty())
        assertEquals(1, cookieJar.loadForRequest(url1).size)
    }

    @Test
    fun cookieJar_handlesDifferentPaths() {
        val url1 = "https://example.com/api/v1".toHttpUrl()
        val url2 = "https://example.com/api/v2".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(url1, listOf(cookie))

        // Same host, different path - should still get cookie (stored by host)
        assertEquals(1, cookieJar.loadForRequest(url2).size)
    }

    @Test
    fun cookieJar_httpAndHttps() {
        val urlHttp = "http://example.com/api".toHttpUrl()
        val urlHttps = "https://example.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc")
            .domain("example.com")
            .path("/")
            .build()

        cookieJar.saveFromResponse(urlHttp, listOf(cookie))

        // Same host regardless of protocol
        assertEquals(1, cookieJar.loadForRequest(urlHttps).size)
    }
}
