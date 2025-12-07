package com.example.dailyinsight.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RetrofitInstance using Robolectric.
 * Uses reflection to reset singleton state between tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RetrofitInstanceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        resetRetrofitInstance()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        resetRetrofitInstance()
    }

    /**
     * Reset RetrofitInstance singleton state using reflection.
     */
    private fun resetRetrofitInstance() {
        try {
            // Reset client field
            val clientField = RetrofitInstance::class.java.getDeclaredField("client")
            clientField.isAccessible = true
            // lateinit var - we can't set to null, but we can check initialization

            // Reset api field
            val apiField = RetrofitInstance::class.java.getDeclaredField("api")
            apiField.isAccessible = true

            // Reset cookieJar field
            val cookieJarField = RetrofitInstance::class.java.getDeclaredField("cookieJar")
            cookieJarField.isAccessible = true

            // For lateinit vars, we need to use the delegate field or leave them
            // The important thing is that init() can be called again
        } catch (e: Exception) {
            // Ignore reflection errors in cleanup
        }
    }

    // ===== init() Tests =====

    @Test
    fun init_initializesApiService() {
        // When: init is called
        RetrofitInstance.init(context)

        // Then: api should be initialized
        assertNotNull(RetrofitInstance.api)
    }

    @Test
    fun init_initializesCookieJar() {
        // When: init is called
        RetrofitInstance.init(context)

        // Then: cookieJar should be initialized
        assertNotNull(RetrofitInstance.cookieJar)
        assertTrue(RetrofitInstance.cookieJar is ProxyCookieJar)
    }

    @Test
    fun init_cookieJarIsProxyCookieJar() {
        // When: init is called
        RetrofitInstance.init(context)

        // Then: cookieJar should be a ProxyCookieJar
        val cookieJar = RetrofitInstance.cookieJar
        assertTrue(cookieJar is ProxyCookieJar)
    }

    @Test
    fun api_afterInit_isApiServiceInstance() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: accessing api
        val api = RetrofitInstance.api

        // Then: Should be an ApiService
        assertNotNull(api)
        assertTrue(api is ApiService)
    }

    // ===== OkHttpClient Configuration Tests =====

    @Test
    fun init_clientHasCookieJar() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: getting the client via reflection
        val clientField = RetrofitInstance::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val client = clientField.get(RetrofitInstance) as OkHttpClient

        // Then: client should have a cookie jar
        assertNotNull(client.cookieJar)
    }

    @Test
    fun init_clientHasInterceptors() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: getting the client via reflection
        val clientField = RetrofitInstance::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val client = clientField.get(RetrofitInstance) as OkHttpClient

        // Then: client should have interceptors (AuthInterceptor, CsrfInterceptor, logging)
        assertTrue(client.interceptors.isNotEmpty())
        assertTrue(client.interceptors.size >= 3)
    }

    // ===== CookieJar Integration Tests =====

    @Test
    fun cookieJar_canSaveAndLoadCookies() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)
        val cookieJar = RetrofitInstance.cookieJar

        // When: saving a cookie
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("example.com")
            .build()
        val cookie = okhttp3.Cookie.Builder()
            .name("test")
            .value("value")
            .domain("example.com")
            .path("/")
            .build()
        cookieJar.saveFromResponse(url, listOf(cookie))

        // Then: cookie should be loadable
        val loaded = cookieJar.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("test", loaded[0].name)
    }

    @Test
    fun cookieJar_clearRemovesCookies() {
        // Given: RetrofitInstance is initialized with cookies
        RetrofitInstance.init(context)
        val cookieJar = RetrofitInstance.cookieJar
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("example.com")
            .build()
        val cookie = okhttp3.Cookie.Builder()
            .name("test")
            .value("value")
            .domain("example.com")
            .path("/")
            .build()
        cookieJar.saveFromResponse(url, listOf(cookie))

        // When: clearing cookies
        cookieJar.clear()

        // Then: no cookies should be loaded
        val loaded = cookieJar.loadForRequest(url)
        assertTrue(loaded.isEmpty())
    }

    // ===== Multiple Init Calls =====

    @Test
    fun init_calledMultipleTimes_doesNotThrow() {
        // When: init is called multiple times
        RetrofitInstance.init(context)
        RetrofitInstance.init(context)

        // Then: should not throw and api should still work
        assertNotNull(RetrofitInstance.api)
    }

    // ===== Interceptor Tests (via reflection) =====

    @Test
    fun authInterceptor_exists() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: getting the client's interceptors
        val clientField = RetrofitInstance::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val client = clientField.get(RetrofitInstance) as OkHttpClient

        // Then: should have AuthInterceptor
        val hasAuthInterceptor = client.interceptors.any {
            it::class.java.simpleName == "AuthInterceptor"
        }
        assertTrue(hasAuthInterceptor)
    }

    @Test
    fun csrfInterceptor_exists() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: getting the client's interceptors
        val clientField = RetrofitInstance::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val client = clientField.get(RetrofitInstance) as OkHttpClient

        // Then: should have CsrfInterceptor
        val hasCsrfInterceptor = client.interceptors.any {
            it::class.java.simpleName == "CsrfInterceptor"
        }
        assertTrue(hasCsrfInterceptor)
    }

    @Test
    fun loggingInterceptor_exists() {
        // Given: RetrofitInstance is initialized
        RetrofitInstance.init(context)

        // When: getting the client's interceptors
        val clientField = RetrofitInstance::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val client = clientField.get(RetrofitInstance) as OkHttpClient

        // Then: should have HttpLoggingInterceptor
        val hasLoggingInterceptor = client.interceptors.any {
            it::class.java.simpleName == "HttpLoggingInterceptor"
        }
        assertTrue(hasLoggingInterceptor)
    }
}
