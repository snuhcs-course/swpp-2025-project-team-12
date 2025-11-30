package com.example.dailyinsight.data.database

import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import org.junit.Assert.*
import org.junit.Test

class CachedHistoryTest {

    private fun createHistoryItem(date: String = "2024-01-01", close: Double = 2500.0) =
        StockIndexHistoryItem(date = date, close = close)

    @Test
    fun cachedHistory_creates() {
        val items = listOf(createHistoryItem())
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = items,
            yearHigh = 3000.0,
            yearLow = 2000.0,
            lastFetched = 1234567890L
        )
        
        assertEquals("KOSPI", cache.indexType)
        assertEquals(1, cache.data.size)
        assertEquals(3000.0, cache.yearHigh, 0.001)
        assertEquals(2000.0, cache.yearLow, 0.001)
        assertEquals(1234567890L, cache.lastFetched)
    }

    @Test
    fun cachedHistory_KOSDAQ() {
        val cache = CachedHistory("KOSDAQ", emptyList(), 1000.0, 500.0, 0L)
        assertEquals("KOSDAQ", cache.indexType)
    }

    @Test
    fun cachedHistory_emptyData() {
        val cache = CachedHistory("KOSPI", emptyList(), 0.0, 0.0, 0L)
        assertTrue(cache.data.isEmpty())
    }

    @Test
    fun cachedHistory_multipleItems() {
        val items = (1..100).map { createHistoryItem("2024-01-$it", 2500.0 + it) }
        val cache = CachedHistory("KOSPI", items, 2600.0, 2500.0, 0L)
        assertEquals(100, cache.data.size)
    }

    @Test
    fun cachedHistory_equality() {
        val items = listOf(createHistoryItem())
        val c1 = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        val c2 = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        assertEquals(c1, c2)
    }

    @Test
    fun cachedHistory_copy() {
        val cache = CachedHistory("KOSPI", emptyList(), 3000.0, 2000.0, 0L)
        val copied = cache.copy(indexType = "KOSDAQ")
        assertEquals("KOSDAQ", copied.indexType)
        assertEquals(3000.0, copied.yearHigh, 0.001)
    }

    @Test
    fun cachedHistory_toString() {
        val cache = CachedHistory("KOSPI", emptyList(), 3000.0, 2000.0, 0L)
        assertNotNull(cache.toString())
        assertTrue(cache.toString().contains("CachedHistory"))
    }

    @Test
    fun cachedHistory_highLow() {
        val cache = CachedHistory("KOSPI", emptyList(), 3000.0, 2000.0, 0L)
        assertTrue(cache.yearHigh > cache.yearLow)
    }

    @Test
    fun cachedHistory_timestamp() {
        val now = System.currentTimeMillis()
        val cache = CachedHistory("KOSPI", emptyList(), 0.0, 0.0, now)
        assertEquals(now, cache.lastFetched)
    }

    @Test
    fun cachedHistory_negativeValues() {
        val cache = CachedHistory("KOSPI", emptyList(), -1.0, -2.0, -1L)
        assertEquals(-1.0, cache.yearHigh, 0.001)
        assertEquals(-2.0, cache.yearLow, 0.001)
    }

    @Test
    fun cachedHistory_largeData() {
        val items = (1..365).map { createHistoryItem("2024-$it", 2500.0) }
        val cache = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        assertEquals(365, cache.data.size)
    }

    @Test
    fun cachedHistory_emptyIndexType() {
        val cache = CachedHistory("", emptyList(), 0.0, 0.0, 0L)
        assertEquals("", cache.indexType)
    }

    @Test
    fun cachedHistory_longIndexType() {
        val longType = "A".repeat(100)
        val cache = CachedHistory(longType, emptyList(), 0.0, 0.0, 0L)
        assertEquals(100, cache.indexType.length)
    }

    @Test
    fun cachedHistory_zeroValues() {
        val cache = CachedHistory("KOSPI", emptyList(), 0.0, 0.0, 0L)
        assertEquals(0.0, cache.yearHigh, 0.001)
        assertEquals(0.0, cache.yearLow, 0.001)
        assertEquals(0L, cache.lastFetched)
    }

    @Test
    fun cachedHistory_maxValues() {
        val cache = CachedHistory("KOSPI", emptyList(), Double.MAX_VALUE, Double.MIN_VALUE, Long.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, cache.yearHigh, 0.001)
        assertEquals(Long.MAX_VALUE, cache.lastFetched)
    }

    @Test
    fun cachedHistory_dataPreservation() {
        val items = listOf(createHistoryItem("2024-01-01", 2500.0))
        val cache = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        assertEquals("2024-01-01", cache.data[0].date)
        assertEquals(2500.0, cache.data[0].close, 0.001)
    }

    @Test
    fun cachedHistory_hashCode() {
        val items = listOf(createHistoryItem())
        val c1 = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        val c2 = CachedHistory("KOSPI", items, 3000.0, 2000.0, 0L)
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test
    fun cachedHistory_component1() {
        val cache = CachedHistory("KOSPI", emptyList(), 3000.0, 2000.0, 0L)
        val (indexType) = cache
        assertEquals("KOSPI", indexType)
    }

    @Test
    fun cachedHistory_allComponents() {
        val items = listOf(createHistoryItem())
        val cache = CachedHistory("KOSPI", items, 3000.0, 2000.0, 123L)
        val (indexType, data, yearHigh, yearLow, lastFetched) = cache
        assertEquals("KOSPI", indexType)
        assertEquals(1, data.size)
        assertEquals(3000.0, yearHigh, 0.001)
        assertEquals(2000.0, yearLow, 0.001)
        assertEquals(123L, lastFetched)
    }

    @Test
    fun cachedHistory_copyAllFields() {
        val cache = CachedHistory("KOSPI", emptyList(), 3000.0, 2000.0, 100L)
        val copied = cache.copy(
            indexType = "KOSDAQ",
            yearHigh = 4000.0,
            yearLow = 1000.0,
            lastFetched = 200L
        )
        assertEquals("KOSDAQ", copied.indexType)
        assertEquals(4000.0, copied.yearHigh, 0.001)
        assertEquals(1000.0, copied.yearLow, 0.001)
        assertEquals(200L, copied.lastFetched)
    }
}