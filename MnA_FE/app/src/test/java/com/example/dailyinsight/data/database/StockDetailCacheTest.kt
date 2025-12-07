package com.example.dailyinsight.data.database

import org.junit.Assert.*
import org.junit.Test

class StockDetailCacheTest {

    @Test
    fun create_withAllFields() {
        val cache = StockDetailCache(
            ticker = "AAPL",
            json = "{\"price\":150}",
            fetchedAt = 1234567890L
        )
        assertEquals("AAPL", cache.ticker)
        assertEquals("{\"price\":150}", cache.json)
        assertEquals(1234567890L, cache.fetchedAt)
    }

    @Test
    fun equality_sameValues() {
        val cache1 = StockDetailCache("AAPL", "{}", 1000L)
        val cache2 = StockDetailCache("AAPL", "{}", 1000L)
        assertEquals(cache1, cache2)
    }

    @Test
    fun inequality_differentTicker() {
        val cache1 = StockDetailCache("AAPL", "{}", 1000L)
        val cache2 = StockDetailCache("GOOG", "{}", 1000L)
        assertNotEquals(cache1, cache2)
    }

    @Test
    fun inequality_differentJson() {
        val cache1 = StockDetailCache("AAPL", "{\"a\":1}", 1000L)
        val cache2 = StockDetailCache("AAPL", "{\"b\":2}", 1000L)
        assertNotEquals(cache1, cache2)
    }

    @Test
    fun inequality_differentFetchedAt() {
        val cache1 = StockDetailCache("AAPL", "{}", 1000L)
        val cache2 = StockDetailCache("AAPL", "{}", 2000L)
        assertNotEquals(cache1, cache2)
    }

    @Test
    fun hashCode_consistency() {
        val cache1 = StockDetailCache("AAPL", "{}", 1000L)
        val cache2 = StockDetailCache("AAPL", "{}", 1000L)
        assertEquals(cache1.hashCode(), cache2.hashCode())
    }

    @Test
    fun copy_modifiesTicker() {
        val original = StockDetailCache("AAPL", "{}", 1000L)
        val copied = original.copy(ticker = "GOOG")
        assertEquals("GOOG", copied.ticker)
        assertEquals("{}", copied.json)
        assertEquals(1000L, copied.fetchedAt)
    }

    @Test
    fun copy_modifiesJson() {
        val original = StockDetailCache("AAPL", "{}", 1000L)
        val copied = original.copy(json = "{\"updated\":true}")
        assertEquals("{\"updated\":true}", copied.json)
    }

    @Test
    fun copy_modifiesFetchedAt() {
        val original = StockDetailCache("AAPL", "{}", 1000L)
        val copied = original.copy(fetchedAt = 2000L)
        assertEquals(2000L, copied.fetchedAt)
    }

    @Test
    fun toString_containsAllFields() {
        val cache = StockDetailCache("AAPL", "{\"test\":1}", 9876543210L)
        val str = cache.toString()
        assertTrue(str.contains("AAPL"))
        assertTrue(str.contains("{\"test\":1}"))
        assertTrue(str.contains("9876543210"))
    }

    @Test
    fun destructuring() {
        val cache = StockDetailCache("AAPL", "{\"data\":\"value\"}", 1000L)
        val (ticker, json, fetchedAt) = cache
        assertEquals("AAPL", ticker)
        assertEquals("{\"data\":\"value\"}", json)
        assertEquals(1000L, fetchedAt)
    }

    @Test
    fun json_canBeEmpty() {
        val cache = StockDetailCache("AAPL", "", 1000L)
        assertEquals("", cache.json)
    }

    @Test
    fun json_canContainComplexData() {
        val complexJson = """{"ticker":"AAPL","profile":{"name":"Apple Inc","sector":"Technology"},"history":[1,2,3]}"""
        val cache = StockDetailCache("AAPL", complexJson, 1000L)
        assertEquals(complexJson, cache.json)
    }
}
