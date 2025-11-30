package com.example.dailyinsight.data

import com.example.dailyinsight.data.database.BriefingCardCache
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.database.FavoriteTicker
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

    // ===== fetchAndSaveBriefing Tests =====

    @Test
    fun fetchAndSaveBriefing_successWithClear_clearsAndInsertsData() = runTest {
        val response = createBriefingListResponse()
        whenever(api.getBriefingList(limit = 10, offset = 0, sort = null)).thenReturn(response)
        whenever(briefingDao.getFavoriteTickers()).thenReturn(emptyList())

        val result = repository.fetchAndSaveBriefing(offset = 0, clear = true)

        assertEquals("2024-01-01", result)
        verify(briefingDao).clearAll()
        verify(briefingDao).insertCards(any())
        verify(briefingDao).syncFavorites()
    }

    @Test
    fun fetchAndSaveBriefing_successWithoutClear_doesNotClearData() = runTest {
        val response = createBriefingListResponse()
        whenever(api.getBriefingList(limit = 10, offset = 10, sort = null)).thenReturn(response)
        whenever(briefingDao.getFavoriteTickers()).thenReturn(emptyList())

        val result = repository.fetchAndSaveBriefing(offset = 10, clear = false)

        assertEquals("2024-01-01", result)
        verify(briefingDao, never()).clearAll()
        verify(briefingDao).insertCards(any())
    }

    @Test
    fun fetchAndSaveBriefing_preservesFavorites() = runTest {
        val response = createBriefingListResponse()
        whenever(api.getBriefingList(limit = 10, offset = 0, sort = null)).thenReturn(response)
        whenever(briefingDao.getFavoriteTickers()).thenReturn(listOf("005930"))

        repository.fetchAndSaveBriefing(offset = 0, clear = true)

        verify(briefingDao).insertCards(argThat { cards ->
            cards.any { it.ticker == "005930" && it.isFavorite }
        })
    }

    @Test
    fun fetchAndSaveBriefing_emptyResponse_doesNotInsert() = runTest {
        val emptyResponse = BriefingListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = null,
            asOf = "2024-01-01"
        )
        whenever(api.getBriefingList(limit = 10, offset = 0, sort = null)).thenReturn(emptyResponse)
        whenever(briefingDao.getFavoriteTickers()).thenReturn(emptyList())

        val result = repository.fetchAndSaveBriefing(offset = 0, clear = false)

        assertEquals("2024-01-01", result)
        verify(briefingDao, never()).insertCards(any())
    }

    @Test
    fun fetchAndSaveBriefing_apiError_returnsNull() = runTest {
        whenever(api.getBriefingList(any(), any(), anyOrNull())).thenAnswer { throw IOException("Network error") }
        whenever(briefingDao.getFavoriteTickers()).thenReturn(emptyList())

        val result = repository.fetchAndSaveBriefing(offset = 0, clear = false)

        assertNull(result)
    }

    @Test
    fun fetchAndSaveBriefing_handlesNullValues() = runTest {
        val response = BriefingListResponse(
            items = listOf(
                BriefingItemDto(
                    ticker = "005930",
                    name = "삼성전자",
                    close = null,
                    change = null,
                    changeRate = null,
                    summary = "요약",
                    overview = null
                )
            ),
            total = 1,
            limit = 10,
            offset = 0,
            source = null,
            asOf = "2024-01-01"
        )
        whenever(api.getBriefingList(limit = 10, offset = 0, sort = null)).thenReturn(response)
        whenever(briefingDao.getFavoriteTickers()).thenReturn(emptyList())

        val result = repository.fetchAndSaveBriefing(offset = 0, clear = false)

        assertNotNull(result)
        verify(briefingDao).insertCards(argThat { cards ->
            cards[0].price == 0L && cards[0].change == 0L && cards[0].changeRate == 0.0
        })
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

    // ===== toggleFavorite Tests =====

    @Test
    fun toggleFavorite_addFavorite_insertsFavoriteAndSyncs() = runTest {
        val result = repository.toggleFavorite("005930", isActive = true)

        assertTrue(result)
        verify(briefingDao).insertFavorite(argThat<FavoriteTicker> { ticker == "005930" })
        verify(briefingDao).syncFavorites()
    }

    @Test
    fun toggleFavorite_removeFavorite_deletesFavoriteAndSyncs() = runTest {
        val result = repository.toggleFavorite("005930", isActive = false)

        assertTrue(result)
        verify(briefingDao).deleteFavorite("005930")
        verify(briefingDao).syncFavorites()
    }

    @Test
    fun toggleFavorite_multipleTickers_worksIndependently() = runTest {
        repository.toggleFavorite("005930", isActive = true)
        repository.toggleFavorite("000660", isActive = true)
        repository.toggleFavorite("005930", isActive = false)

        verify(briefingDao, times(2)).insertFavorite(any())
        verify(briefingDao, times(1)).deleteFavorite("005930")
        verify(briefingDao, times(3)).syncFavorites()
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

    private fun createBriefingListResponse() = BriefingListResponse(
        items = listOf(
            BriefingItemDto(
                ticker = "005930",
                name = "삼성전자",
                close = "72000",
                change = "1000",
                changeRate = "1.5",
                summary = "삼성전자 요약",
                overview = null
            ),
            BriefingItemDto(
                ticker = "000660",
                name = "SK하이닉스",
                close = "150000",
                change = "-2000",
                changeRate = "-1.3",
                summary = "SK하이닉스 요약",
                overview = null
            )
        ),
        total = 2,
        limit = 10,
        offset = 0,
        source = "cache",
        asOf = "2024-01-01"
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