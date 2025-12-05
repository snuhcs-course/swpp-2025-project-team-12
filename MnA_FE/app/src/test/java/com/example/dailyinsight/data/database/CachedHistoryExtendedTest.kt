package com.example.dailyinsight.data.database

import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import org.junit.Assert.*
import org.junit.Test

class CachedHistoryExtendedTest {

    private fun createHistoryItem(date: String, close: Double) = StockIndexHistoryItem(
        date = date,
        close = close
    )

    @Test
    fun cachedHistory_withAllFields_createsCorrectly() {
        val historyItems = listOf(
            createHistoryItem("2024-01-01", 2500.0),
            createHistoryItem("2024-01-02", 2520.0)
        )

        val cache = CachedHistory(
            indexType = "KOSPI",
            data = historyItems,
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1705312800000L
        )

        assertEquals("KOSPI", cache.indexType)
        assertEquals(2, cache.data.size)
        assertEquals(2800.0, cache.yearHigh, 0.01)
        assertEquals(2200.0, cache.yearLow, 0.01)
        assertEquals(1705312800000L, cache.lastFetched)
    }

    @Test
    fun cachedHistory_KOSPI_createsCorrectly() {
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = System.currentTimeMillis()
        )

        assertEquals("KOSPI", cache.indexType)
    }

    @Test
    fun cachedHistory_KOSDAQ_createsCorrectly() {
        val cache = CachedHistory(
            indexType = "KOSDAQ",
            data = emptyList(),
            yearHigh = 900.0,
            yearLow = 700.0,
            lastFetched = System.currentTimeMillis()
        )

        assertEquals("KOSDAQ", cache.indexType)
    }

    @Test
    fun cachedHistory_emptyData_createsCorrectly() {
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 0.0,
            yearLow = 0.0,
            lastFetched = 0L
        )

        assertTrue(cache.data.isEmpty())
    }

    @Test
    fun cachedHistory_withLargeDataset() {
        val historyItems = (1..365).map { day ->
            createHistoryItem("2024-${(day / 30 + 1).coerceAtMost(12)}-${(day % 28 + 1)}", 2500.0 + day)
        }

        val cache = CachedHistory(
            indexType = "KOSPI",
            data = historyItems,
            yearHigh = 2865.0,
            yearLow = 2501.0,
            lastFetched = System.currentTimeMillis()
        )

        assertEquals(365, cache.data.size)
    }

    @Test
    fun cachedHistory_equality() {
        val historyItems = listOf(createHistoryItem("2024-01-01", 2500.0))

        val cache1 = CachedHistory(
            indexType = "KOSPI",
            data = historyItems,
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )
        val cache2 = CachedHistory(
            indexType = "KOSPI",
            data = historyItems,
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        assertEquals(cache1, cache2)
    }

    @Test
    fun cachedHistory_inequality_differentIndexType() {
        val cache1 = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )
        val cache2 = CachedHistory(
            indexType = "KOSDAQ",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        assertNotEquals(cache1, cache2)
    }

    @Test
    fun cachedHistory_inequality_differentYearHigh() {
        val cache1 = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )
        val cache2 = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2900.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        assertNotEquals(cache1, cache2)
    }

    @Test
    fun cachedHistory_hashCode() {
        val cache1 = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )
        val cache2 = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        assertEquals(cache1.hashCode(), cache2.hashCode())
    }

    @Test
    fun cachedHistory_copy_modifiesIndexType() {
        val original = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val copied = original.copy(indexType = "KOSDAQ")

        assertEquals("KOSDAQ", copied.indexType)
        assertEquals(2800.0, copied.yearHigh, 0.01)
    }

    @Test
    fun cachedHistory_copy_modifiesYearHigh() {
        val original = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val copied = original.copy(yearHigh = 3000.0)

        assertEquals(3000.0, copied.yearHigh, 0.01)
        assertEquals("KOSPI", copied.indexType)
    }

    @Test
    fun cachedHistory_copy_modifiesLastFetched() {
        val original = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val copied = original.copy(lastFetched = 2000L)

        assertEquals(2000L, copied.lastFetched)
    }

    @Test
    fun cachedHistory_toString_containsFields() {
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val str = cache.toString()
        assertTrue(str.contains("KOSPI"))
        assertTrue(str.contains("2800"))
    }

    @Test
    fun cachedHistory_destructuring() {
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val (indexType, data, yearHigh, yearLow, lastFetched) = cache

        assertEquals("KOSPI", indexType)
        assertTrue(data.isEmpty())
        assertEquals(2800.0, yearHigh, 0.01)
        assertEquals(2200.0, yearLow, 0.01)
        assertEquals(1000L, lastFetched)
    }

    @Test
    fun cachedHistory_yearRange_calculation() {
        val cache = CachedHistory(
            indexType = "KOSPI",
            data = emptyList(),
            yearHigh = 2800.0,
            yearLow = 2200.0,
            lastFetched = 1000L
        )

        val range = cache.yearHigh - cache.yearLow
        assertEquals(600.0, range, 0.01)
    }

    @Test
    fun cachedHistory_dataAccessByIndex() {
        val historyItems = listOf(
            createHistoryItem("2024-01-01", 2500.0),
            createHistoryItem("2024-01-02", 2520.0),
            createHistoryItem("2024-01-03", 2510.0)
        )

        val cache = CachedHistory(
            indexType = "KOSPI",
            data = historyItems,
            yearHigh = 2520.0,
            yearLow = 2500.0,
            lastFetched = 1000L
        )

        assertEquals("2024-01-01", cache.data[0].date)
        assertEquals(2520.0, cache.data[1].close, 0.01)
        assertEquals("2024-01-03", cache.data[2].date)
    }
}
