package com.example.dailyinsight.data.database

import org.junit.Assert.*
import org.junit.Test

class FavoriteTickerTest {

    @Test
    fun create_withAllFields() {
        val ticker = FavoriteTicker(
            ticker = "AAPL",
            username = "testUser",
            timestamp = 1234567890L
        )
        assertEquals("AAPL", ticker.ticker)
        assertEquals("testUser", ticker.username)
        assertEquals(1234567890L, ticker.timestamp)
    }

    @Test
    fun create_withDefaultTimestamp() {
        val before = System.currentTimeMillis()
        val ticker = FavoriteTicker(
            ticker = "GOOG",
            username = "user1"
        )
        val after = System.currentTimeMillis()

        assertEquals("GOOG", ticker.ticker)
        assertEquals("user1", ticker.username)
        assertTrue(ticker.timestamp >= before)
        assertTrue(ticker.timestamp <= after)
    }

    @Test
    fun equality_sameValues() {
        val ticker1 = FavoriteTicker("MSFT", "user", 1000L)
        val ticker2 = FavoriteTicker("MSFT", "user", 1000L)
        assertEquals(ticker1, ticker2)
    }

    @Test
    fun inequality_differentTicker() {
        val ticker1 = FavoriteTicker("AAPL", "user", 1000L)
        val ticker2 = FavoriteTicker("GOOG", "user", 1000L)
        assertNotEquals(ticker1, ticker2)
    }

    @Test
    fun inequality_differentUsername() {
        val ticker1 = FavoriteTicker("AAPL", "user1", 1000L)
        val ticker2 = FavoriteTicker("AAPL", "user2", 1000L)
        assertNotEquals(ticker1, ticker2)
    }

    @Test
    fun inequality_differentTimestamp() {
        val ticker1 = FavoriteTicker("AAPL", "user", 1000L)
        val ticker2 = FavoriteTicker("AAPL", "user", 2000L)
        assertNotEquals(ticker1, ticker2)
    }

    @Test
    fun hashCode_consistency() {
        val ticker1 = FavoriteTicker("AAPL", "user", 1000L)
        val ticker2 = FavoriteTicker("AAPL", "user", 1000L)
        assertEquals(ticker1.hashCode(), ticker2.hashCode())
    }

    @Test
    fun copy_modifiesTicker() {
        val original = FavoriteTicker("AAPL", "user", 1000L)
        val copied = original.copy(ticker = "GOOG")
        assertEquals("GOOG", copied.ticker)
        assertEquals("user", copied.username)
        assertEquals(1000L, copied.timestamp)
    }

    @Test
    fun copy_modifiesUsername() {
        val original = FavoriteTicker("AAPL", "user1", 1000L)
        val copied = original.copy(username = "user2")
        assertEquals("AAPL", copied.ticker)
        assertEquals("user2", copied.username)
    }

    @Test
    fun copy_modifiesTimestamp() {
        val original = FavoriteTicker("AAPL", "user", 1000L)
        val copied = original.copy(timestamp = 2000L)
        assertEquals(2000L, copied.timestamp)
    }

    @Test
    fun toString_containsAllFields() {
        val ticker = FavoriteTicker("AAPL", "testUser", 1234567890L)
        val str = ticker.toString()
        assertTrue(str.contains("AAPL"))
        assertTrue(str.contains("testUser"))
        assertTrue(str.contains("1234567890"))
    }

    @Test
    fun destructuring() {
        val ticker = FavoriteTicker("AAPL", "user", 1000L)
        val (t, u, ts) = ticker
        assertEquals("AAPL", t)
        assertEquals("user", u)
        assertEquals(1000L, ts)
    }
}
