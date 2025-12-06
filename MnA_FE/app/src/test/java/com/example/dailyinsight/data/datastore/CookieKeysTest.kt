package com.example.dailyinsight.data.datastore

import org.junit.Assert.*
import org.junit.Test

class CookieKeysTest {

    @Test
    fun csrfToken_hasCorrectName() {
        assertEquals("csrftoken", CookieKeys.CSRF_TOKEN.name)
    }

    @Test
    fun accessToken_hasCorrectName() {
        assertEquals("access_token", CookieKeys.ACCESS_TOKEN.name)
    }

    @Test
    fun refreshToken_hasCorrectName() {
        assertEquals("refresh_token", CookieKeys.REFRESH_TOKEN.name)
    }

    @Test
    fun username_hasCorrectName() {
        assertEquals("username", CookieKeys.USERNAME.name)
    }

    @Test
    fun allKeys_areDifferent() {
        val keys = listOf(
            CookieKeys.CSRF_TOKEN,
            CookieKeys.ACCESS_TOKEN,
            CookieKeys.REFRESH_TOKEN,
            CookieKeys.USERNAME
        )
        assertEquals(4, keys.toSet().size)
    }
}
