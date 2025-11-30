package com.example.dailyinsight.data.repository

import com.example.dailyinsight.data.database.CachedHistory
import com.example.dailyinsight.data.database.HistoryCacheDao
import com.example.dailyinsight.data.dto.*
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

@ExperimentalCoroutinesApi
class MarketIndexRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var historyCacheDao: HistoryCacheDao
    private lateinit var repository: MarketIndexRepository

    @Before
    fun setup() {
        apiService = mock()
        historyCacheDao = mock()
        repository = MarketIndexRepository(apiService, historyCacheDao)
    }

    // ===== getMarketData Tests =====

    @Test
    fun getMarketData_returnsDataWithNames() = runTest {
        val response = StockIndexLatestResponse(
            status = "success",
            data = mapOf(
                "KOSPI" to StockIndexData(
                    name = "",
                    close = 2500.0,
                    changeAmount = 10.0,
                    changePercent = 0.4,
                    date = "2024-01-01",
                    high = 2510.0,
                    low = 2490.0,
                    open = 2495.0,
                    volume = 1000000L
                ),
                "KOSDAQ" to StockIndexData(
                    name = "",
                    close = 800.0,
                    changeAmount = -5.0,
                    changePercent = -0.6,
                    date = "2024-01-01",
                    high = 810.0,
                    low = 795.0,
                    open = 805.0,
                    volume = 500000L
                )
            )
        )
        whenever(apiService.getStockIndex()).thenReturn(response)

        val result = repository.getMarketData()

        assertEquals(2, result.size)
        assertEquals("KOSPI", result["KOSPI"]?.name)
        assertEquals("KOSDAQ", result["KOSDAQ"]?.name)
        assertEquals(2500.0, result["KOSPI"]?.close)
    }

    @Test
    fun getMarketData_emptyResponse_returnsEmptyMap() = runTest {
        val response = StockIndexLatestResponse(status = "success", data = emptyMap())
        whenever(apiService.getStockIndex()).thenReturn(response)

        val result = repository.getMarketData()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getMarketData_apiError_throwsException() = runTest {
        whenever(apiService.getStockIndex()).thenAnswer { throw IOException("Network error") }

        try {
            repository.getMarketData()
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== getLLMSummaryLatest Tests =====

    @Test
    fun getLLMSummaryLatest_parsesDoubleEncodedJson() = runTest {
        // 서버가 JSON을 문자열로 한번 더 감싸서 보내는 경우
        val innerJson = """{"asof_date":"2024-01-01","basic_overview":"시장 요약"}"""
        val doubleEncoded = "\"${innerJson.replace("\"", "\\\"")}\""
        whenever(apiService.getLLMSummaryLatest()).thenReturn(doubleEncoded.toResponseBody())

        val result = repository.getLLMSummaryLatest()

        assertEquals("2024-01-01", result.asofDate)
        assertEquals("시장 요약", result.basicOverview)
    }

    @Test
    fun getLLMSummaryLatest_apiError_throwsException() = runTest {
        whenever(apiService.getLLMSummaryLatest()).thenAnswer { throw IOException("Network error") }

        try {
            repository.getLLMSummaryLatest()
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== getHistoryCacheFlow Tests =====

    @Test
    fun getHistoryCacheFlow_returnsFlowFromDao() = runTest {
        val cached = CachedHistory(
            indexType = "KOSPI",
            data = listOf(StockIndexHistoryItem("2024-01-01", 2500.0)),
            yearHigh = 2600.0,
            yearLow = 2400.0,
            lastFetched = System.currentTimeMillis()
        )
        whenever(historyCacheDao.getHistoryCacheFlow("KOSPI")).thenReturn(flowOf(cached))

        val result = repository.getHistoryCacheFlow("KOSPI").first()

        assertNotNull(result)
        assertEquals("KOSPI", result?.indexType)
        assertEquals(2600.0, result?.yearHigh)
    }

    @Test
    fun getHistoryCacheFlow_returnsNullWhenNoCache() = runTest {
        whenever(historyCacheDao.getHistoryCacheFlow("KOSPI")).thenReturn(flowOf(null))

        val result = repository.getHistoryCacheFlow("KOSPI").first()

        assertNull(result)
    }

    // ===== refreshHistoricalData Tests =====

    @Test
    fun refreshHistoricalData_fetchesAndCachesData() = runTest {
        val historyResponse = StockIndexHistoryResponse(
            status = "success",
            index = "KOSPI",
            data = listOf(
                StockIndexHistoryItem("2024-01-01", 2500.0),
                StockIndexHistoryItem("2024-01-02", 2520.0),
                StockIndexHistoryItem("2024-01-03", 2480.0)
            )
        )
        whenever(apiService.getHistoricalData("KOSPI", 365)).thenReturn(historyResponse)

        repository.refreshHistoricalData("KOSPI")

        verify(historyCacheDao).insertHistory(argThat { cache ->
            cache.indexType == "KOSPI" &&
            cache.yearHigh == 2520.0 &&
            cache.yearLow == 2480.0 &&
            cache.data.size == 3
        })
    }

    @Test
    fun refreshHistoricalData_emptyResponse_doesNotInsert() = runTest {
        val emptyResponse = StockIndexHistoryResponse(status = "success", index = "KOSPI", data = emptyList())
        whenever(apiService.getHistoricalData("KOSPI", 365)).thenReturn(emptyResponse)

        repository.refreshHistoricalData("KOSPI")

        verify(historyCacheDao, never()).insertHistory(any())
    }

    @Test
    fun refreshHistoricalData_apiError_doesNotCrash() = runTest {
        whenever(apiService.getHistoricalData("KOSPI", 365)).thenAnswer { throw IOException("Network error") }

        // Should not throw
        repository.refreshHistoricalData("KOSPI")

        verify(historyCacheDao, never()).insertHistory(any())
    }

    @Test
    fun refreshHistoricalData_calculatesYearHighAndLow() = runTest {
        val historyResponse = StockIndexHistoryResponse(
            status = "success",
            index = "KOSDAQ",
            data = listOf(
                StockIndexHistoryItem("2024-01-01", 100.0),
                StockIndexHistoryItem("2024-01-02", 200.0),
                StockIndexHistoryItem("2024-01-03", 50.0),
                StockIndexHistoryItem("2024-01-04", 150.0)
            )
        )
        whenever(apiService.getHistoricalData("KOSDAQ", 365)).thenReturn(historyResponse)

        repository.refreshHistoricalData("KOSDAQ")

        verify(historyCacheDao).insertHistory(argThat { cache ->
            cache.yearHigh == 200.0 && cache.yearLow == 50.0
        })
    }
}