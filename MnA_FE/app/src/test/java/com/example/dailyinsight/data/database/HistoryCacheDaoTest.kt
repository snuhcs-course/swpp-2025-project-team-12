package com.example.dailyinsight.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for HistoryCacheDao using Room in-memory database with Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryCacheDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: HistoryCacheDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.historyCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createCachedHistory(
        indexType: String,
        data: List<StockIndexHistoryItem> = listOf(
            StockIndexHistoryItem("2024-01-01", 2500.0),
            StockIndexHistoryItem("2024-01-02", 2550.0)
        ),
        yearHigh: Double = 2800.0,
        yearLow: Double = 2200.0,
        lastFetched: Long = System.currentTimeMillis()
    ) = CachedHistory(
        indexType = indexType,
        data = data,
        yearHigh = yearHigh,
        yearLow = yearLow,
        lastFetched = lastFetched
    )

    // ===== insertHistory Tests =====

    @Test
    fun insertHistory_insertsSuccessfully() = runTest {
        val cache = createCachedHistory("KOSPI")

        dao.insertHistory(cache)

        val result = dao.getHistoryCacheOnce("KOSPI")
        assertNotNull(result)
        assertEquals("KOSPI", result?.indexType)
    }

    @Test
    fun insertHistory_replacesOnConflict() = runTest {
        val original = createCachedHistory("KOSPI", yearHigh = 2500.0)
        val updated = createCachedHistory("KOSPI", yearHigh = 3000.0)

        dao.insertHistory(original)
        dao.insertHistory(updated)

        val result = dao.getHistoryCacheOnce("KOSPI")
        assertEquals(3000.0, result?.yearHigh)
    }

    // ===== getHistoryCacheOnce Tests =====

    @Test
    fun getHistoryCacheOnce_withExistingType_returnsCache() = runTest {
        val cache = createCachedHistory(
            indexType = "KOSDAQ",
            yearHigh = 900.0,
            yearLow = 700.0
        )
        dao.insertHistory(cache)

        val result = dao.getHistoryCacheOnce("KOSDAQ")

        assertNotNull(result)
        assertEquals("KOSDAQ", result?.indexType)
        assertEquals(900.0, result?.yearHigh)
        assertEquals(700.0, result?.yearLow)
    }

    @Test
    fun getHistoryCacheOnce_withNonExistingType_returnsNull() = runTest {
        val result = dao.getHistoryCacheOnce("NONEXISTENT")

        assertNull(result)
    }

    @Test
    fun getHistoryCacheOnce_returnsCorrectData() = runTest {
        val historyItems = listOf(
            StockIndexHistoryItem("2024-01-01", 2500.0),
            StockIndexHistoryItem("2024-01-02", 2550.0),
            StockIndexHistoryItem("2024-01-03", 2600.0)
        )
        val cache = createCachedHistory("KOSPI", data = historyItems)
        dao.insertHistory(cache)

        val result = dao.getHistoryCacheOnce("KOSPI")

        assertEquals(3, result?.data?.size)
        assertEquals("2024-01-01", result?.data?.get(0)?.date)
        assertEquals(2500.0, result?.data?.get(0)?.close)
    }

    // ===== getHistoryCacheFlow Tests =====

    @Test
    fun getHistoryCacheFlow_emitsUpdates() = runTest {
        val cache = createCachedHistory("KOSPI")
        dao.insertHistory(cache)

        val result = dao.getHistoryCacheFlow("KOSPI").first()

        assertNotNull(result)
        assertEquals("KOSPI", result?.indexType)
    }

    @Test
    fun getHistoryCacheFlow_emitsNullWhenEmpty() = runTest {
        val result = dao.getHistoryCacheFlow("KOSPI").first()

        assertNull(result)
    }

    @Test
    fun getHistoryCacheFlow_separatesIndexTypes() = runTest {
        dao.insertHistory(createCachedHistory("KOSPI", yearHigh = 2800.0))
        dao.insertHistory(createCachedHistory("KOSDAQ", yearHigh = 900.0))

        val kospi = dao.getHistoryCacheFlow("KOSPI").first()
        val kosdaq = dao.getHistoryCacheFlow("KOSDAQ").first()

        assertEquals(2800.0, kospi?.yearHigh)
        assertEquals(900.0, kosdaq?.yearHigh)
    }

    // ===== Multiple Index Types Tests =====

    @Test
    fun multipleIndexTypes_storedSeparately() = runTest {
        dao.insertHistory(createCachedHistory("KOSPI"))
        dao.insertHistory(createCachedHistory("KOSDAQ"))
        dao.insertHistory(createCachedHistory("KOSPI200"))

        assertNotNull(dao.getHistoryCacheOnce("KOSPI"))
        assertNotNull(dao.getHistoryCacheOnce("KOSDAQ"))
        assertNotNull(dao.getHistoryCacheOnce("KOSPI200"))
    }

    // ===== lastFetched Tests =====

    @Test
    fun lastFetched_storedCorrectly() = runTest {
        val timestamp = 1234567890123L
        val cache = createCachedHistory("KOSPI", lastFetched = timestamp)

        dao.insertHistory(cache)

        val result = dao.getHistoryCacheOnce("KOSPI")
        assertEquals(timestamp, result?.lastFetched)
    }
}
