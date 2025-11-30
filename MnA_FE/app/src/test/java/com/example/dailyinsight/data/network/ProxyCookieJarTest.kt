package com.example.dailyinsight.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ProxyCookieJarTest {

    private lateinit var realCookieJar: MyCookieJar
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var proxyCookieJar: ProxyCookieJar
    private val testUrl = "https://example.com/api".toHttpUrl()

    @Before
    fun setup() {
        realCookieJar = mock()
        mockDataStore = mock()
        
        // 기본 DataStore mock 설정 - 빈 preferences 반환
        whenever(mockDataStore.data).thenReturn(flowOf(preferencesOf()))
        
        proxyCookieJar = ProxyCookieJar(realCookieJar, mockDataStore)
    }

    // ===== saveFromResponse Tests =====

    @Test
    fun saveFromResponse_delegatesToRealCookieJar() {
        val cookies = listOf(createCookie("token", "abc123"))

        proxyCookieJar.saveFromResponse(testUrl, cookies)

        verify(realCookieJar).saveFromResponse(testUrl, cookies)
    }

    @Test
    fun saveFromResponse_withMultipleCookies_savesAll() {
        val cookies = listOf(
            createCookie("access_token", "access123"),
            createCookie("refresh_token", "refresh456")
        )

        proxyCookieJar.saveFromResponse(testUrl, cookies)

        verify(realCookieJar).saveFromResponse(testUrl, cookies)
    }

    @Test
    fun saveFromResponse_withEmptyCookies_stillDelegates() {
        val cookies = emptyList<Cookie>()

        proxyCookieJar.saveFromResponse(testUrl, cookies)

        verify(realCookieJar).saveFromResponse(testUrl, cookies)
    }

    // ===== loadForRequest Tests - Memory First =====

    @Test
    fun loadForRequest_returnsFromMemoryWhenAvailable() {
        val memoryCookies = listOf(createCookie("session", "mem123"))
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(memoryCookies)

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertEquals(memoryCookies, result)
        // DataStore는 건드리지 않음
    }

    @Test
    fun loadForRequest_returnsMultipleCookiesFromMemory() {
        val memoryCookies = listOf(
            createCookie("token1", "value1"),
            createCookie("token2", "value2")
        )
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(memoryCookies)

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertEquals(2, result.size)
    }

    // ===== loadForRequest Tests - DataStore Fallback =====

    @Test
    fun loadForRequest_fallsBackToDataStoreWhenMemoryEmpty() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        
        val prefs = preferencesOf(
            stringPreferencesKey("access_token") to "stored_access",
            stringPreferencesKey("refresh_token") to "stored_refresh"
        )
        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "access_token" && it.value == "stored_access" })
        assertTrue(result.any { it.name == "refresh_token" && it.value == "stored_refresh" })
    }

    @Test
    fun loadForRequest_onlyAccessToken_returnsOneCookie() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        
        val prefs = preferencesOf(
            stringPreferencesKey("access_token") to "only_access"
        )
        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertEquals(1, result.size)
        assertEquals("access_token", result[0].name)
    }

    @Test
    fun loadForRequest_onlyRefreshToken_returnsOneCookie() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        
        val prefs = preferencesOf(
            stringPreferencesKey("refresh_token") to "only_refresh"
        )
        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertEquals(1, result.size)
        assertEquals("refresh_token", result[0].name)
    }

    @Test
    fun loadForRequest_noTokensInDataStore_returnsEmptyList() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        whenever(mockDataStore.data).thenReturn(flowOf(preferencesOf()))

        val result = proxyCookieJar.loadForRequest(testUrl)

        assertTrue(result.isEmpty())
    }

    @Test
    fun loadForRequest_savesToRealCookieJarAfterDataStoreLoad() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        
        val prefs = preferencesOf(
            stringPreferencesKey("access_token") to "access123"
        )
        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        proxyCookieJar.loadForRequest(testUrl)

        verify(realCookieJar).saveFromResponse(eq(testUrl), argThat { cookies ->
            cookies.size == 1 && cookies[0].name == "access_token"
        })
    }

    @Test
    fun loadForRequest_cookieHasCorrectProperties() {
        whenever(realCookieJar.loadForRequest(testUrl)).thenReturn(emptyList())
        
        val prefs = preferencesOf(
            stringPreferencesKey("access_token") to "test_value"
        )
        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        val result = proxyCookieJar.loadForRequest(testUrl)

        val cookie = result[0]
        assertEquals("access_token", cookie.name)
        assertEquals("test_value", cookie.value)
        assertEquals(testUrl.host, cookie.domain)
        assertEquals("/", cookie.path)
        assertTrue(cookie.httpOnly)
        assertTrue(cookie.secure)
    }

    // ===== clear Tests =====

    @Test
    fun clear_delegatesToRealCookieJar() {
        proxyCookieJar.clear()

        verify(realCookieJar).clear()
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