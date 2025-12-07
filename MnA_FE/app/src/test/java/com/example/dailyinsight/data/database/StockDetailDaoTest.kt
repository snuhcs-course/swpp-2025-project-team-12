package com.example.dailyinsight.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for StockDetailDao using Room in-memory database with Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StockDetailDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: StockDetailDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.stockDetailDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ===== insertDetail Tests =====

    @Test
    fun insertDetail_insertsSuccessfully() = runTest {
        val cache = StockDetailCache("AAPL", """{"ticker":"AAPL"}""", System.currentTimeMillis())

        dao.insertDetail(cache)

        val result = dao.getDetail("AAPL")
        assertNotNull(result)
        assertEquals("AAPL", result?.ticker)
    }

    @Test
    fun insertDetail_replacesOnConflict() = runTest {
        val original = StockDetailCache("AAPL", """{"name":"Apple"}""", 1000L)
        val updated = StockDetailCache("AAPL", """{"name":"Apple Inc"}""", 2000L)

        dao.insertDetail(original)
        dao.insertDetail(updated)

        val result = dao.getDetail("AAPL")
        assertEquals("""{"name":"Apple Inc"}""", result?.json)
        assertEquals(2000L, result?.fetchedAt)
    }

    // ===== getDetail Tests =====

    @Test
    fun getDetail_withExistingTicker_returnsCache() = runTest {
        val cache = StockDetailCache("GOOGL", """{"ticker":"GOOGL"}""", 1234567890L)
        dao.insertDetail(cache)

        val result = dao.getDetail("GOOGL")

        assertNotNull(result)
        assertEquals("GOOGL", result?.ticker)
        assertEquals("""{"ticker":"GOOGL"}""", result?.json)
        assertEquals(1234567890L, result?.fetchedAt)
    }

    @Test
    fun getDetail_withNonExistingTicker_returnsNull() = runTest {
        val result = dao.getDetail("NONEXISTENT")

        assertNull(result)
    }

    @Test
    fun getDetail_afterInsertMultiple_returnsCorrectOne() = runTest {
        dao.insertDetail(StockDetailCache("AAPL", """{"name":"Apple"}""", 1000L))
        dao.insertDetail(StockDetailCache("GOOGL", """{"name":"Google"}""", 2000L))
        dao.insertDetail(StockDetailCache("MSFT", """{"name":"Microsoft"}""", 3000L))

        val result = dao.getDetail("GOOGL")

        assertEquals("GOOGL", result?.ticker)
        assertEquals("""{"name":"Google"}""", result?.json)
    }

    // ===== clearAll Tests =====

    @Test
    fun clearAll_removesAllEntries() = runTest {
        dao.insertDetail(StockDetailCache("AAPL", """{}""", 1000L))
        dao.insertDetail(StockDetailCache("GOOGL", """{}""", 2000L))

        dao.clearAll()

        assertNull(dao.getDetail("AAPL"))
        assertNull(dao.getDetail("GOOGL"))
    }

    @Test
    fun clearAll_onEmptyTable_succeeds() = runTest {
        // Should not throw
        dao.clearAll()

        assertNull(dao.getDetail("ANYTHING"))
    }
}
