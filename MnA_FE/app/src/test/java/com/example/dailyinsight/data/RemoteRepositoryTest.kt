package com.example.dailyinsight.data

import com.example.dailyinsight.data.database.BriefingCardCache
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.database.StockDetailCache
import com.example.dailyinsight.data.database.StockDetailDao
import com.example.dailyinsight.data.dto.*
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

@ExperimentalCoroutinesApi
class RemoteRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var briefingDao: BriefingDao
    private lateinit var stockDetailDao: StockDetailDao
    private lateinit var repository: RemoteRepository

    @Before
    fun setup() {
        api = mock()
        briefingDao = mock()
        stockDetailDao = mock()
        repository = RemoteRepository(api, briefingDao, stockDetailDao)
    }

    // ===== getBriefingFlow Tests =====

    @Test
    fun getBriefingFlow_returnsFlowFromDao() = runTest {
        val cachedItems = listOf(
            createBriefingCardCache("005930", "삼성전자"),
            createBriefingCardCache("000660", "SK하이닉스")
        )
        whenever(briefingDao.getAllCards()).thenReturn(flowOf(cachedItems))

        val result = repository.getBriefingFlow().first()

        assertEquals(2, result.size)
        assertEquals("삼성전자", result[0].name)
        assertEquals("SK하이닉스", result[1].name)
    }

    @Test
    fun getBriefingFlow_returnsEmptyListWhenDaoEmpty() = runTest {
        whenever(briefingDao.getAllCards()).thenReturn(flowOf(emptyList()))

        val result = repository.getBriefingFlow().first()

        assertTrue(result.isEmpty())
    }

    // ===== getStockReport Tests =====

    @Test
    fun getStockReport_returnsCachedDataWhenAvailable() = runTest {
        val cached = createStockDetailCache("005930")
        whenever(stockDetailDao.getDetail("005930")).thenReturn(cached)

        val result = repository.getStockReport("005930")

        assertEquals("005930", result.ticker)
        verify(api, never()).getStockReport(any())
    }

    @Test
    fun getStockReport_fetchesFromApiWhenNotCached() = runTest {
        val apiResponse = createStockDetailDto("005930")
        whenever(stockDetailDao.getDetail("005930")).thenReturn(null)
        whenever(api.getStockReport("005930")).thenReturn(apiResponse)

        val result = repository.getStockReport("005930")

        assertEquals("005930", result.ticker)
        verify(api).getStockReport("005930")
        verify(stockDetailDao).insertDetail(any())
    }

    @Test
    fun getStockReport_savesToCacheAfterApiFetch() = runTest {
        val apiResponse = createStockDetailDto("005930")
        whenever(stockDetailDao.getDetail("005930")).thenReturn(null)
        whenever(api.getStockReport("005930")).thenReturn(apiResponse)

        repository.getStockReport("005930")

        verify(stockDetailDao).insertDetail(argThat { ticker == "005930" })
    }

    @Test
    fun getStockReport_throwsExceptionOnApiError() = runTest {
        whenever(stockDetailDao.getDetail("005930")).thenReturn(null)
        whenever(api.getStockReport("005930")).thenAnswer { throw IOException("Network error") }

        try {
            repository.getStockReport("005930")
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== getStockOverview Tests =====

    @Test
    fun getStockOverview_returnsApiResponse() = runTest {
        val overview = StockOverviewDto(
            asOfDate = "2024-01-01",
            summary = "테스트 요약",
            fundamental = "펀더멘털 분석",
            technical = "기술적 분석",
            news = listOf("뉴스1", "뉴스2")
        )
        whenever(api.getStockOverview("005930")).thenReturn(overview)

        val result = repository.getStockOverview("005930")

        assertEquals("테스트 요약", result.summary)
        assertEquals("펀더멘털 분석", result.fundamental)
    }

    @Test
    fun getStockOverview_throwsExceptionOnApiError() = runTest {
        whenever(api.getStockOverview("005930")).thenAnswer { throw IOException("Network error") }

        try {
            repository.getStockOverview("005930")
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== Helper Functions =====

    private fun createBriefingCardCache(ticker: String, name: String) = BriefingCardCache(
        ticker = ticker,
        name = name,
        price = 70000L,
        change = 1000L,
        changeRate = 1.5,
        headline = "테스트 요약",
        label = null,
        confidence = null,
        fetchedAt = System.currentTimeMillis()
    )

    private fun createStockDetailDto(ticker: String) = StockDetailDto(
        ticker = ticker,
        name = "테스트 주식",
        current = CurrentData(
            price = 72000,
            change = -100,
            changeRate = -0.14,
            marketCap = 1000000,
            date = "2024-01-01"
        ),
        valuation = ValuationData(
            peTtm = 15.5,
            priceToBook = 1.2,
            bps = 60000
        ),
        dividend = DividendData(
            `yield` = 2.5
        ),
        financials = FinancialsData(
            eps = 4800,
            dps = 1800,
            roe = 8.0
        ),
        history = listOf(
            HistoryItem(date = "2024-01-01", close = 71000.0),
            HistoryItem(date = "2024-01-02", close = 72000.0)
        ),
        profile = ProfileData(
            explanation = "테스트 회사 설명"
        ),
        asOf = "2024-01-01"
    )

    private fun createStockDetailCache(ticker: String) = StockDetailCache(
        ticker = ticker,
        json = """
            {
                "ticker": "$ticker",
                "name": "캐시된 주식",
                "current": {"price": 72000},
                "history": []
            }
        """.trimIndent(),
        fetchedAt = System.currentTimeMillis()
    )
}